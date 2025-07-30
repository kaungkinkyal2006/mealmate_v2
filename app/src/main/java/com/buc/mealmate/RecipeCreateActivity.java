package com.buc.mealmate;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.buc.mealmate.data.AppDatabase;
import com.buc.mealmate.data.Recipe;
import com.buc.mealmate.databinding.ActivityRecipeCreateBinding;

import java.util.ArrayList;
import java.util.List;

public class RecipeCreateActivity extends AppCompatActivity {
    private ActivityRecipeCreateBinding binding;
    private AppDatabase db;

    private List<String> ingredientList;
    private RecipeAdapter.IngredientAdapter ingredientAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecipeCreateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);

        ingredientList = new ArrayList<>();
        ingredientAdapter = new RecipeAdapter.IngredientAdapter(ingredientList, position -> {
            ingredientList.remove(position);
            ingredientAdapter.notifyItemRemoved(position);
            ingredientAdapter.notifyItemRangeChanged(position, ingredientList.size());
        });

        binding.rvIngredients.setLayoutManager(new LinearLayoutManager(this));
        binding.rvIngredients.setAdapter(ingredientAdapter);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.white));
        window.setNavigationBarColor(getResources().getColor(R.color.primaryCoral));

// For light status bar icons (black icons for better contrast on light orange):
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        } else {
            View decor = window.getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // Add ingredient button
        binding.btnAddIngredient.setOnClickListener(v -> {
            String ingredient = binding.ingredientInput.getText().toString().trim();
            if (TextUtils.isEmpty(ingredient)) {
                Toast.makeText(this, "Enter an ingredient", Toast.LENGTH_SHORT).show();
                return;
            }
            ingredientList.add(ingredient);
            ingredientAdapter.notifyItemInserted(ingredientList.size() - 1);
            binding.ingredientInput.setText("");
        });

        // Save recipe button
        binding.saveRecipeButton.setOnClickListener(v -> {
            String name = binding.recipeNameInput.getText().toString().trim();
            String instructions = binding.instructionsInput.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter recipe name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ingredientList.isEmpty()) {
                Toast.makeText(this, "Add at least one ingredient", Toast.LENGTH_SHORT).show();
                return;
            }

            if (instructions.isEmpty()) {
                Toast.makeText(this, "Enter preparation instructions", Toast.LENGTH_SHORT).show();
                return;
            }

            // Join ingredients list to comma-separated string
            StringBuilder ingredientsJoined = new StringBuilder();
            for (int i = 0; i < ingredientList.size(); i++) {
                ingredientsJoined.append(ingredientList.get(i));
                if (i < ingredientList.size() - 1) {
                    ingredientsJoined.append(",");
                }
            }

            Recipe recipe = new Recipe(name, ingredientsJoined.toString(), instructions);

            new Thread(() -> {
                db.recipeDao().insert(recipe);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Recipe saved", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }).start();
        });
    }
}
