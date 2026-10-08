package soa.lab2.starship.service;

import soa.lab2.starship.model.SpaceMarine;
import soa.lab2.starship.dto.MeleeWeaponDTO;
import soa.lab2.starship.dto.WeaponDTO;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class CrewFilters {
    private static final Set<String> STRING_FIELDS = Set.of("name", "achievements",
            "chapter.name", "chapter.parentLegion", "chapter.world");

    private CrewFilters() {
    }

    record Condition(String field, String operator, Comparable<?> value) {
    }

    static List<Condition> parse(List<String> filters) {
        var result = new ArrayList<Condition>();
        if (filters == null) return result;
        for (String filter : filters) {
            String[] parts = filter.split(",", 3);
            if (parts.length != 3 || !Set.of("eq", "gt", "lt").contains(parts[1]))
                throw new IllegalArgumentException("Фильтр должен иметь формат: поле,eq|gt|lt,значение");
            result.add(new Condition(parts[0], parts[1], parseValue(parts[0], parts[2])));
        }
        return result;
    }

    private static Comparable<?> parseValue(String field, String text) {
        try {
            if (STRING_FIELDS.contains(field)) return text;
            return switch (field) {
                case "weaponType" -> WeaponDTO.fromValue(text).getValue();
                case "meleeWeapon" -> MeleeWeaponDTO.fromValue(text).getValue();
                case "id" -> Long.valueOf(text);
                case "health", "coordinates.y" -> Integer.valueOf(text);
                case "coordinates.x" -> {
                    double value = Double.parseDouble(text);
                    if (!Double.isFinite(value)) throw new IllegalArgumentException();
                    yield value;
                }
                case "creationDate" -> OffsetDateTime.parse(text);
                default -> throw new IllegalArgumentException("Неизвестное поле: " + field);
            };
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Некорректное значение фильтра для поля " + field + ": " + text, ex);
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Неизвестное поле:")) throw ex;
            throw new IllegalArgumentException("Некорректное значение фильтра для поля " + field + ": " + text, ex);
        }
    }

    static boolean matches(SpaceMarine marine, List<Condition> conditions) {
        for (Condition condition : conditions) {
            Comparable<?> actual = fieldValue(marine, condition.field());
            if (actual == null) return false;
            int comparison = compare(actual, condition.value());
            if (condition.operator().equals("eq") && comparison != 0
                    || condition.operator().equals("gt") && comparison <= 0
                    || condition.operator().equals("lt") && comparison >= 0) return false;
        }
        return true;
    }

    private static Comparable<?> fieldValue(SpaceMarine marine, String field) {
        return switch (field) {
            case "id" -> marine.id();
            case "name" -> marine.name();
            case "creationDate" -> marine.creationDate();
            case "health" -> marine.health();
            case "achievements" -> marine.achievements();
            case "weaponType" -> marine.weaponType();
            case "meleeWeapon" -> marine.meleeWeapon();
            case "coordinates.x" -> marine.coordinates() == null ? null : marine.coordinates().x();
            case "coordinates.y" -> marine.coordinates() == null ? null : marine.coordinates().y();
            case "chapter.name" -> marine.chapter() == null ? null : marine.chapter().name();
            case "chapter.parentLegion" -> marine.chapter() == null ? null : marine.chapter().parentLegion();
            case "chapter.world" -> marine.chapter() == null ? null : marine.chapter().world();
            default -> throw new IllegalArgumentException("Неизвестное поле: " + field);
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compare(Comparable actual, Comparable expected) {
        return actual.compareTo(expected);
    }
}
