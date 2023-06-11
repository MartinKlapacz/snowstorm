package org.snomed.snowstorm.avelios;

public enum TranslationMethod {
    TRANSLATION_METHOD_NONE,
    TRANSLATION_METHOD_RULE_BASED,
    TRANSLATION_METHOD_KNOWLEDGE_INPUT_MAPPING,
    TRANSLATION_METHOD_FUZZY_TOKEN_MATCHING,
    TRANSLATION_METHOD_ICD10_MAPPING,
    TRANSLATION_METHOD_ALPHAID_MAPPING,
    TRANSLATION_METHOD_ORPHAID_MAPPING;

    static TranslationMethod saveValueOf(String method) {
        for (TranslationMethod methodEnumValue: TranslationMethod.values()) {
            if (methodEnumValue.toString().equals(method)) {
                return methodEnumValue;
            }
        }
        return TRANSLATION_METHOD_NONE;
    }

}
