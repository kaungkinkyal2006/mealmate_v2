package com.buc.mealmate.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recipes")
public class Recipe {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String ingredients;  // Comma-separated list of ingredients
    public String purchasedIngredients; // Comma-separated purchased items
    public String instructions;

    public Recipe(String name, String ingredients, String instructions) {
        this.name = name;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.purchasedIngredients = ""; // Initially none purchased
    }

    public boolean isReadyToCook() {
        if (ingredients == null || ingredients.trim().isEmpty()) return false;

        String[] all = ingredients.split(",");
        String[] purchased = purchasedIngredients == null ? new String[]{} : purchasedIngredients.split(",");

        // Trim spaces from ingredients
        for (int i = 0; i < all.length; i++) {
            all[i] = all[i].trim();
        }
        for (int i = 0; i < purchased.length; i++) {
            purchased[i] = purchased[i].trim();
        }

        // Check that all purchased are contained in all (order may differ)
        if (purchased.length != all.length) return false;

        for (String ing : all) {
            boolean found = false;
            for (String pur : purchased) {
                if (ing.equalsIgnoreCase(pur)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

}
