package com.elephantjava.service;

import java.util.Map;

/**
 * 小程序动物标识与物种中文名。名称与 miniprogram/services/animals.js 一致。
 */
public final class MiniProgramAnimals {

    private static final Map<String, String> SPECIES_NAMES = Map.of(
            "asian-elephant", "亚洲象",
            "giant-panda", "大熊猫",
            "giraffe", "长颈鹿",
            "chimpanzee", "黑猩猩",
            "proboscis-monkey", "长鼻猴",
            "koala", "考拉",
            "white-tiger", "白虎",
            "lion", "狮子",
            "flamingo", "火烈鸟",
            "white-rhino", "白犀牛"
    );

    private MiniProgramAnimals() {
    }

    public static String speciesName(String animalId) {
        if (animalId == null) {
            return null;
        }
        return SPECIES_NAMES.get(animalId.trim());
    }
}
