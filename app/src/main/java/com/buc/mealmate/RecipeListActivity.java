package com.buc.mealmate;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.buc.mealmate.data.AppDatabase;
import com.buc.mealmate.data.Recipe;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;


public class RecipeListActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private AppDatabase db;
    private View btnAddRecipe, btnSendSms, btnCancelSelection;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;

    private static final String PREFS_NAME = "MealMatePrefs";
    private static final String KEY_LOGGED_IN_USER = "loggedInUser";

    private boolean isInSelectionMode = false;

    // For pending SMS after permission granted
    private List<Recipe> pendingSmsRecipes;
    private String pendingSmsPhoneNumber;

    private static final int SMS_PERMISSION_REQUEST_CODE = 101;

    private SensorManager sensorManager;
    private float accelCurrent;
    private float accelLast;
    private float shake;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_list);

        db = AppDatabase.getInstance(this);

        setupStatusBar();
        setupToolbarAndDrawer();
        setupViews();
        setupListeners();
    }

    private void setupStatusBar() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(R.color.red));
        window.setNavigationBarColor(getColor(R.color.white));

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

    private void setupToolbarAndDrawer() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // Set username in navigation header
        View headerView = navigationView.getHeaderView(0);
        TextView tvUsernameHeader = headerView.findViewById(R.id.tvUsernameHeader);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String username = prefs.getString(KEY_LOGGED_IN_USER, "User");
        tvUsernameHeader.setText(getString(R.string.welcome_user, username));
    }

    private void setupViews() {
        recyclerView = findViewById(R.id.recyclerViewRecipes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnAddRecipe = findViewById(R.id.btnAddRecipe);
        btnSendSms = findViewById(R.id.btnSendSms);
        btnCancelSelection = findViewById(R.id.btnCancelSelection);
        btnCancelSelection.setVisibility(View.GONE);

        btnSendSms.setEnabled(false);
        btnSendSms.setTag("Select Recipes");

        loadRecipes();

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Recipe recipe = adapter.getRecipeAt(position);

                if (direction == ItemTouchHelper.LEFT) {
                    confirmDelete(recipe);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    Intent editIntent = new Intent(RecipeListActivity.this, RecipeDetailActivity.class);
                    editIntent.putExtra("recipe_id", recipe.id); // You need to handle this in RecipeCreateActivity
                    startActivity(editIntent);
                }

                adapter.notifyItemChanged(position); // to undo swipe animation
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);

    }

    private void setupListeners() {
        btnAddRecipe.setOnClickListener(v -> {
            startActivity(new Intent(this, RecipeCreateActivity.class));
        });

        btnSendSms.setOnClickListener(v -> {
            if (adapter == null) {
                Toast.makeText(this, "No recipes loaded", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isInSelectionMode) {
                enterSelectionMode();
            } else {
                List<Recipe> selectedRecipes = adapter.getSelectedRecipes();
                if (selectedRecipes.isEmpty()) {
                    Toast.makeText(this, "Please select at least one recipe.", Toast.LENGTH_SHORT).show();
                    return;
                }
                promptPhoneNumberAndSendSms(selectedRecipes);
            }
        });

        btnCancelSelection.setOnClickListener(v -> {
            exitSelectionMode();
            Toast.makeText(this, "Selection cancelled", Toast.LENGTH_SHORT).show();
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelCurrent = SensorManager.GRAVITY_EARTH;
        accelLast = SensorManager.GRAVITY_EARTH;
        shake = 0.00f;

    }


    private void enterSelectionMode() {
        isInSelectionMode = true;
        adapter.setSelectionMode(true);
        btnSendSms.setTag("Send SMS");
        if (btnSendSms instanceof TextView) {
            ((TextView) btnSendSms).setText("Send SMS");
        }
        btnCancelSelection.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Select recipes to send SMS", Toast.LENGTH_SHORT).show();
    }

    private void exitSelectionMode() {
        isInSelectionMode = false;
        if (adapter != null) {
            adapter.setSelectionMode(false);
            adapter.clearSelection();
        }
        btnSendSms.setTag("Select Recipes");
        if (btnSendSms instanceof TextView) {
            ((TextView) btnSendSms).setText("Select Recipes");
        }
        btnCancelSelection.setVisibility(View.GONE);

        pendingSmsPhoneNumber = null;
        pendingSmsRecipes = null;
    }


    private void promptPhoneNumberAndSendSms(List<Recipe> selectedRecipes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter phone number");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String phoneNumber = input.getText().toString().trim();
            if (phoneNumber.isEmpty()) {
                Toast.makeText(this, "Phone number cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        SMS_PERMISSION_REQUEST_CODE);

                pendingSmsPhoneNumber = phoneNumber;
                pendingSmsRecipes = selectedRecipes;
            } else {
                sendSmsToSelected(phoneNumber, selectedRecipes);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void sendSmsToSelected(String phoneNumber, List<Recipe> selectedRecipes) {
        SmsManager smsManager = SmsManager.getDefault();

        for (Recipe recipe : selectedRecipes) {
            StringBuilder message = new StringBuilder();
            message.append("Recipe: ").append(recipe.name).append("\n");

            if (recipe.ingredients != null && !recipe.ingredients.isEmpty()) {
                String formattedIngredients = recipe.ingredients.replaceAll("\\s*,\\s*", ", ");
                message.append("Ingredients: ").append(formattedIngredients).append("\n");
            }

            if (recipe.instructions != null && !recipe.instructions.isEmpty()) {
                message.append("Instructions: ").append(recipe.instructions);
            }

            List<String> parts = smsManager.divideMessage(message.toString());
            smsManager.sendMultipartTextMessage(phoneNumber, null, new ArrayList<>(parts), null, null);
        }

        exitSelectionMode();
        Toast.makeText(this, "SMS sent to selected recipes", Toast.LENGTH_SHORT).show();
    }

    private void loadRecipes() {
        List<Recipe> recipes = db.recipeDao().getAll();
        if (adapter == null) {
            adapter = new RecipeAdapter(this, recipes, new RecipeAdapter.OnRecipeDeleteListener() {
                @Override
                public void onRecipeDelete(Recipe recipe) {
                    // optional callback
                }

                @Override
                public void onDeleteClicked(Recipe recipe) {
                    confirmDelete(recipe);
                }
            });
            recyclerView.setAdapter(adapter);
        } else {
            adapter.setRecipes(recipes);
            adapter.notifyDataSetChanged();
        }
        btnSendSms.setEnabled(!recipes.isEmpty());
    }

    private void confirmDelete(Recipe recipe) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Recipe")
                .setMessage("Are you sure you want to delete \"" + recipe.name + "\"?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    db.recipeDao().delete(recipe);
                    Toast.makeText(this, "Recipe deleted", Toast.LENGTH_SHORT).show();
                    loadRecipes();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted. Sending SMS...", Toast.LENGTH_SHORT).show();
                if (pendingSmsPhoneNumber != null && pendingSmsRecipes != null) {
                    sendSmsToSelected(pendingSmsPhoneNumber, pendingSmsRecipes);
                }
            } else {
                Toast.makeText(this, "SMS Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecipes();
        sensorManager.registerListener(sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorListener);
    }

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            accelLast = accelCurrent;
            accelCurrent = (float) Math.sqrt(x * x + y * y + z * z);
            float delta = accelCurrent - accelLast;
            shake = shake * 0.9f + delta;

            if (shake > 12) {
                if (isInSelectionMode) {
                    runOnUiThread(() -> {
                        exitSelectionMode();
                        Toast.makeText(RecipeListActivity.this, "Shake detected: Selection cancelled", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        loadRecipes();
                        Toast.makeText(RecipeListActivity.this, "Shake detected: Recipes refreshed", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().remove(KEY_LOGGED_IN_USER).apply();

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }

        drawerLayout.closeDrawers();
        return true;
    }
}
