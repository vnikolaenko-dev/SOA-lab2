package soa.lab2.collection.repository;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import soa.lab2.collection.model.*;

public final class SpaceMarineSpecifications {
    private SpaceMarineSpecifications() {}
    private static final Map<String, Class<?>> FIELDS = Map.ofEntries(
            Map.entry("id", Long.class), Map.entry("creationDate", OffsetDateTime.class),
            Map.entry("name", String.class), Map.entry("health", Integer.class),
            Map.entry("achievements", String.class), Map.entry("weaponType", Weapon.class),
            Map.entry("meleeWeapon", MeleeWeapon.class),
            Map.entry("coordinates.x", Double.class), Map.entry("coordinates.y", Integer.class),
            Map.entry("chapter.name", String.class), Map.entry("chapter.parentLegion", String.class),
            Map.entry("chapter.world", String.class));

    private static Class<?> type(String field) {
        var type = FIELDS.get(field);
        if (type == null) throw new IllegalArgumentException("Неизвестное поле: " + field);
        return type;
    }

    private static Comparable<?> value(String field, String text) {
        Class<?> type = type(field);
        try {
            if (type == Integer.class) return Integer.valueOf(text);
            if (type == Long.class) return Long.valueOf(text);
            if (type == Double.class) {
                double d = Double.parseDouble(text);
                if (!Double.isFinite(d)) throw new IllegalArgumentException("Значение должно быть конечным числом");
                return d;
            }
            if (type == OffsetDateTime.class) return OffsetDateTime.parse(text);
            if (type == Weapon.class) return Weapon.valueOf(text);
            if (type == MeleeWeapon.class) return MeleeWeapon.valueOf(text);
            return text;
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Некорректная дата: " + text, ex);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Некорректное значение фильтра для поля " + field + ": " + text, ex);
        }
    }

    // Значения перечислений хранятся строками; сравнение выполняется по их именам в API.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Specification<SpaceMarine> condition(String field, String operator, Comparable<?> value) {
        return (root, query, builder) -> {
            Path<?> path = root;
            for (String part : field.split("\\.")) path = path.get(part);
            if (operator.equals("eq")) return builder.equal(path, value);
            Expression expression = path;
            Comparable comparison = value;
            if (value instanceof Enum<?> enumValue) {
                expression = path.as(String.class);
                comparison = enumValue.name();
            }
            return operator.equals("gt") ? builder.greaterThan(expression, comparison)
                    : builder.lessThan(expression, comparison);
        };
    }

    public static Specification<SpaceMarine> filters(List<String> filters) {
        Specification<SpaceMarine> result = (root, query, builder) -> builder.conjunction();
        if (filters != null) for (String item : filters) {
            String[] parts = item.split(",", 3);
            if (parts.length != 3 || !Set.of("eq", "gt", "lt").contains(parts[1])) {
                throw new IllegalArgumentException("Фильтр должен иметь формат: поле,eq|gt|lt,значение");
            }
            result = result.and(condition(parts[0], parts[1], value(parts[0], parts[2])));
        }
        return result;
    }

    public static Sort sort(List<String> sorts) {
        var orders = new ArrayList<Sort.Order>();
        if (sorts != null) for (String item : sorts) {
            String[] parts = item.split(",", -1);
            if (parts.length > 2 || parts.length == 2 && !Set.of("asc", "desc").contains(parts[1])) {
                throw new IllegalArgumentException("Сортировка должна иметь формат: поле[,asc|desc]");
            }
            type(parts[0]);
            orders.add(new Sort.Order(parts.length == 2 && parts[1].equals("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC, parts[0]));
        }
        orders.add(Sort.Order.asc("id"));
        return Sort.by(orders);
    }
}
