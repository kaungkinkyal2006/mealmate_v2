package com.buc.mealmate;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.buc.mealmate.data.AppDatabase;
import com.buc.mealmate.data.Recipe;
import com.buc.mealmate.databinding.ActivityRecipeDetailBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecipeDetailActivity extends AppCompatActivity {
    private ActivityRecipeDetailBinding binding;
    private AppDatabase db;
    private Recipe recipe;
    private List<CheckBox> ingredientCheckboxes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecipeDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);

        int recipeId = getIntent().getIntExtra("recipe_id", -1);
        if (recipeId == -1) {
            Toast.makeText(this, "Invalid recipe", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupSystemBars();

        recipe = db.recipeDao().getById(recipeId);
        if (recipe == null) {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.tvRecipeName.setText(recipe.name);
        binding.tvInstructions.setText(recipe.instructions);

        setupIngredientCheckboxes();

        binding.btnSave.setOnClickListener(v -> {
            updatePurchasedIngredients();
            db.recipeDao().update(recipe);
            Toast.makeText(this, recipe.isReadyToCook() ?
                    "All ingredients purchased! Ready to cook." :
                    "Progress saved.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupSystemBars() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.primaryCoral));

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
    }

    private void setupIngredientCheckboxes() {
        LinearLayout container = binding.ingredientsContainer;
        container.removeAllViews();
        ingredientCheckboxes.clear();

        List<String> allIngredients = Arrays.asList(recipe.ingredients.split(","));
        List<String> purchased = recipe.purchasedIngredients == null ?
                new ArrayList<>() :
                Arrays.asList(recipe.purchasedIngredients.split(","));

        List<String> purchasedTrimmed = new ArrayList<>();
        for (String p : purchased) {
            purchasedTrimmed.add(p.trim());
        }

        for (String ingredient : allIngredients) {
            String trimmed = ingredient.trim();

            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setGravity(Gravity.CENTER_VERTICAL);
            itemLayout.setPadding(16, 16, 16, 16);

            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            itemParams.setMargins(0, 0, 0, 16);
            itemLayout.setLayoutParams(itemParams);

            CheckBox cb = new CheckBox(this);
            cb.setText(trimmed);
            cb.setTextSize(16);
            cb.setButtonTintList(ContextCompat.getColorStateList(this, R.color.primaryCoral));
            cb.setChecked(purchasedTrimmed.contains(trimmed));

            TextView chip = new TextView(this);
            chip.setText("Purchased");
            chip.setTextColor(Color.WHITE);
            chip.setTextSize(12);
            chip.setPadding(32, 12, 32, 12);
            chip.setBackgroundResource(R.drawable.chip_green);
            chip.setVisibility(cb.isChecked() ? View.VISIBLE : View.GONE);

            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            chipParams.setMargins(24, 0, 0, 0);
            chip.setLayoutParams(chipParams);

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                chip.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });

            itemLayout.addView(cb);
            itemLayout.addView(chip);
            container.addView(itemLayout);

            ingredientCheckboxes.add(cb);
        }
    }


    private void updatePurchasedIngredients() {
        List<String> purchasedNow = new ArrayList<>();
        for (CheckBox cb : ingredientCheckboxes) {
            if (cb.isChecked()) {
                purchasedNow.add(cb.getText().toString().trim());
            }
        }
        recipe.purchasedIngredients = String.join(",", purchasedNow);
    }
}
