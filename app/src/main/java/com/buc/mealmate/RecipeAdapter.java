package com.buc.mealmate;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.buc.mealmate.data.Recipe;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    private Context context;
    private List<Recipe> recipes;
    private OnRecipeDeleteListener deleteListener;

    // Track selected positions for multi-selection
    private Set<Integer> selectedPositions = new HashSet<>();

    // Flag to control checkbox visibility
    private boolean selectionMode = false;

    // Interface for delete callback
    public interface OnRecipeDeleteListener {
        void onRecipeDelete(Recipe recipe);
        void onDeleteClicked(Recipe recipe);
    }

    public RecipeAdapter(Context context, List<Recipe> recipes, OnRecipeDeleteListener deleteListener) {
        this.context = context;
        this.recipes = recipes;
        this.deleteListener = deleteListener;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
        clearSelection();
        notifyDataSetChanged();
    }

    // Enable or disable selection mode (show/hide checkboxes)
    public void setSelectionMode(boolean enabled) {
        this.selectionMode = enabled;
        if (!enabled) {
            clearSelection();
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public void clearSelection() {
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    // Return the list of selected recipes
    public List<Recipe> getSelectedRecipes() {
        List<Recipe> selected = new java.util.ArrayList<>();
        for (Integer pos : selectedPositions) {
            if (pos >= 0 && pos < recipes.size()) {
                selected.add(recipes.get(pos));
            }
        }
        return selected;
    }

    @NonNull
    @Override
    public RecipeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeAdapter.ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.tvName.setText(recipe.name);
        holder.tvStatus.setText(recipe.isReadyToCook() ? "Ready to Cook" : "Not Ready");

        // Show or hide checkbox based on selectionMode
        holder.checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);

        // Avoid triggering listener during recycling
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedPositions.contains(position));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPositions.add(position);
            } else {
                selectedPositions.remove(position);
            }
        });

        // Item click: toggle checkbox if selection mode, else open detail
        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                boolean newChecked = !holder.checkBox.isChecked();
                holder.checkBox.setChecked(newChecked);
            } else {
                Intent intent = new Intent(context, RecipeDetailActivity.class);
                intent.putExtra("recipe_id", recipe.id);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public Recipe getRecipeAt(int position) {
        return recipes.get(position);
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRecipeName);
            tvStatus = itemView.findViewById(R.id.tvRecipeStatus);
            checkBox = itemView.findViewById(R.id.checkboxSelect);
        }
    }

    // Nested IngredientAdapter
    public static class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.ViewHolder> {
        private List<String> ingredients;
        private OnIngredientDeleteListener deleteListener;

        public interface OnIngredientDeleteListener {
            void onIngredientDelete(int position);
        }

        public IngredientAdapter(List<String> ingredients, OnIngredientDeleteListener deleteListener) {
            this.ingredients = ingredients;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public IngredientAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ingredient, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull IngredientAdapter.ViewHolder holder, int position) {
            String ingredient = ingredients.get(position);
            holder.tvIngredient.setText(ingredient);
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onIngredientDelete(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return ingredients.size();
        }


        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvIngredient;
            ImageButton btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvIngredient = itemView.findViewById(R.id.tvIngredient);
                btnDelete = itemView.findViewById(R.id.btnDeleteIngredient);
            }
        }
    }
}
