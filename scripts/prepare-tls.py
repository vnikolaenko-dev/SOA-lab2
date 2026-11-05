#!/usr/bin/env python3
"""Validate issued certificates and build the TLS stores consumed by Compose."""
import os
from pathlib import Path
import secrets
import shlex
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
CERTS = ROOT / 'certs'
JAVA_SERVICES = ('config-server', 'eureka-server', 'api-gateway', 'collection-service', 'starship-service')
SERVICES = (*JAVA_SERVICES, 'haproxy', 'caddy', 'soa-front')


def run(*args, **kwargs):
    return subprocess.run(args, check=True, capture_output=True, **kwargs).stdout


def main():
    for tool in ('openssl', 'keytool'):
        if not shutil.which(tool):
            raise SystemExit(f'Required executable missing: {tool}')
    ca = CERTS / 'ca'
    for name in SERVICES:
        folder = CERTS / name
        run('openssl', 'verify', '-CAfile', str(ca / 'root-ca.crt'),
            '-untrusted', str(ca / 'intermediates.crt'), '-purpose', 'sslserver',
            '-verify_hostname', name, str(folder / 'tls.crt'))
        key_pub = run('openssl', 'pkey', '-in', str(folder / 'tls.key'), '-pubout')
        cert_pub = run('openssl', 'x509', '-in', str(folder / 'tls.crt'), '-pubkey', '-noout')
        if key_pub != cert_pub:
            raise SystemExit(f'Certificate and key do not match: {name}')
        (folder / 'fullchain.pem').write_bytes(
            (folder / 'tls.crt').read_bytes().rstrip() + b'\n'
            + (ca / 'intermediates.crt').read_bytes().rstrip() + b'\n')
    env_file = ROOT / '.env'
    env_text = env_file.read_text() if env_file.exists() else ''
    env_password = None
    for line in env_text.splitlines():
        if line.strip().startswith('TLS_STORE_PASSWORD='):
            tokens = shlex.split(line.strip().split('=', 1)[1], comments=True)
            env_password = tokens[0] if tokens else None
    password = os.environ.get('TLS_STORE_PASSWORD') or env_password
    if not password:
        password = secrets.token_hex(24)
        with env_file.open('a') as f:
            if env_text and not env_text.endswith('\n'):
                f.write('\n')
            f.write(f'TLS_STORE_PASSWORD={password}\n')
        env_file.chmod(0o600)
    os.environ['SOA_TLS_STORE_PASSWORD'] = password
    if '\n' in password or '\r' in password:
        raise SystemExit('TLS_STORE_PASSWORD must not contain newlines')
    if len(password) < 6:
        raise SystemExit('TLS_STORE_PASSWORD must contain at least 6 characters (keytool requirement)')
    # Build each store in a private temporary directory before replacing the old file.
    with tempfile.TemporaryDirectory() as temp:
        temp = Path(temp)
        for name in JAVA_SERVICES:
            target = temp / f'{name}.p12'
            run('openssl', 'pkcs12', '-export', '-name', name,
                '-in', str(CERTS / name / 'tls.crt'), '-inkey', str(CERTS / name / 'tls.key'),
                '-certfile', str(ca / 'intermediates.crt'), '-out', str(target),
                '-passout', 'env:SOA_TLS_STORE_PASSWORD')
            target.chmod(0o600)
            shutil.copy2(target, CERTS / name / 'keystore.p12')
        trust = temp / 'truststore.p12'
        run('keytool', '-importcert', '-noprompt', '-alias', 'openbao-root',
            '-file', str(ca / 'root-ca.crt'), '-keystore', str(trust),
            '-storetype', 'PKCS12', '-storepass:env', 'SOA_TLS_STORE_PASSWORD')
        trust.chmod(0o600)
        shutil.copy2(trust, ca / 'truststore.p12')
    options = ca / 'java-tls.options'
    escaped_password = password.replace('\\', '\\\\').replace('"', '\\"')
    with options.open('w') as f:
        options.chmod(0o600)
        f.write('-Djavax.net.ssl.trustStore=/certs/ca/truststore.p12\n')
        f.write('-Djavax.net.ssl.trustStoreType=PKCS12\n')
        f.write(f'"-Djavax.net.ssl.trustStorePassword={escaped_password}"\n')
    combined = CERTS / 'haproxy' / 'server.pem'
    # HAProxy expects the certificate chain and private key in a single PEM file.
    with combined.open('w') as f:
        combined.chmod(0o600)
        f.write((CERTS / 'haproxy' / 'fullchain.pem').read_text().rstrip() + '\n')
        f.write((CERTS / 'haproxy' / 'tls.key').read_text())
    print('TLS ready: 5 PKCS12 keystores, CA truststore, HAProxy PEM.')
    print('TLS_STORE_PASSWORD is read from the environment or .env; no passwords are printed.')


if __name__ == '__main__':
    main()
