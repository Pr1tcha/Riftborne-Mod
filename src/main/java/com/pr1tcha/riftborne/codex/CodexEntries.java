package com.pr1tcha.riftborne.codex;

import com.pr1tcha.riftborne.codex.data.CodexEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CodexEntries {
    private static final Map<String, CodexEntry> ENTRIES = new LinkedHashMap<>();

    static {
        register(new CodexEntry("rna_overview", "Резонансная нейроархитектура", "РНА",
                "Внутренняя структура, проводящая и стабилизирующая резонансные импульсы.", false));
        register(new CodexEntry("node_density", "Плотность узлов", "РНА",
                "Определяет насыщенность узловой структуры и её базовую вместимость.", false));
        register(new CodexEntry("connectivity", "Связность", "РНА",
                "Показывает качество соединений и чистоту передачи импульса.", true));
        register(new CodexEntry("throughput", "Пропускная способность", "РНА",
                "Определяет объём нагрузки, который архитектура проводит за короткое время.", true));
        register(new CodexEntry("overload_resistance", "Устойчивость к перегрузке", "РНА",
                "Снижает повреждение архитектуры при чрезмерной резонансной нагрузке.", true));
        register(new CodexEntry("meta_wear", "Мета-износ", "Мета-износ",
                "Накопленное повреждение РНА. Это не энергия: высокий износ угрожает распадом.", true));
    }

    private CodexEntries() {
    }

    private static void register(CodexEntry entry) {
        ENTRIES.put(entry.id(), entry);
    }

    public static List<CodexEntry> all() {
        return List.copyOf(ENTRIES.values());
    }

    public static CodexEntry get(String id) {
        return ENTRIES.get(id);
    }
}
