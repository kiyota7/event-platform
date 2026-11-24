package com.example.eventplatform.event;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 初回起動時に初期カテゴリを投入する */
@Component
public class CategorySeeder implements ApplicationRunner {
    private final CategoryRepository categories;
    public CategorySeeder(CategoryRepository categories) { this.categories = categories; }

    @Override
    public void run(ApplicationArguments args) {
        if (categories.count() == 0)
            categories.saveAll(List.of("勉強会", "セミナー", "ハンズオン", "交流会").stream().map(Category::new).toList());
    }
}
