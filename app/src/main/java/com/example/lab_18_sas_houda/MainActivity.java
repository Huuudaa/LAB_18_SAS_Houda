package com.example.lab_18_sas_houda;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.lab_18_sas_houda.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private CounterViewModel viewModel;

    // Mode classique : variable d'instance classique (perdue à la rotation)
    private int classicCount = 0;
    private boolean isViewModelMode = false;

    // Journaliseur de logs
    private final StringBuilder logsBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Récupération ou création du ViewModel lié au cycle de vie
        viewModel = new ViewModelProvider(this).get(CounterViewModel.class);

        // Affiche le Hashcode de l'Activity actuelle
        String activityHash = Integer.toHexString(System.identityHashCode(this));
        binding.tvActivityHash.setText("MainActivity@" + activityHash);

        // Enregistre l'événement onCreate
        logEvent("onCreate()");

        // Restauration de l'état si disponible
        if (savedInstanceState != null) {
            isViewModelMode = savedInstanceState.getBoolean("mode_key", false);
            classicCount = savedInstanceState.getInt("classic_count_key", 0);
            boolean saveStateChecked = savedInstanceState.getBoolean("save_state_checked_key", false);
            binding.switchSaveState.setChecked(saveStateChecked);

            String savedLogs = savedInstanceState.getString("logs_key", "");
            if (!savedLogs.isEmpty()) {
                logsBuilder.append(savedLogs);
                binding.tvConsoleLogs.setText(logsBuilder.toString());
            }
            logEvent("État restauré depuis savedInstanceState");
        }

        // 2. Observation du LiveData (réactif et sensible au cycle de vie)
        viewModel.getCount().observe(this, count -> {
            if (isViewModelMode) {
                binding.tvCount.setText(String.valueOf(count));
                logEvent("[LiveData] Compteur mis à jour : " + count);
            }
        });

        // Observation du statut de chargement pour le thread background
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isViewModelMode) {
                binding.pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                binding.btnBackgroundThread.setEnabled(!isLoading);
            }
        });

        // 3. Configuration des Écouteurs d'Événements

        // Bascule entre les modes Sans ViewModel et Avec ViewModel
        binding.toggleGroupMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnModeClassic) {
                    setMode(false);
                } else if (checkedId == R.id.btnModeViewModel) {
                    setMode(true);
                }
            }
        });

        // Actions du compteur (séparation de la logique selon le mode)
        binding.btnIncrement.setOnClickListener(v -> {
            if (isViewModelMode) {
                viewModel.increment();
            } else {
                classicCount++;
                updateUI();
                logEvent("[Classique] Incrémentation manuelle. Nouveau compte: " + classicCount);
            }
        });

        binding.btnDecrement.setOnClickListener(v -> {
            if (isViewModelMode) {
                viewModel.decrement();
            } else {
                classicCount--;
                updateUI();
                logEvent("[Classique] Décrémentation manuelle. Nouveau compte: " + classicCount);
            }
        });

        binding.btnReset.setOnClickListener(v -> {
            if (isViewModelMode) {
                viewModel.reset();
            } else {
                classicCount = 0;
                updateUI();
                logEvent("[Classique] Réinitialisation.");
            }
        });

        // Lancement du thread background (seulement disponible en mode ViewModel)
        binding.btnBackgroundThread.setOnClickListener(v -> {
            logEvent("Simulation Thread Background démarrée (1 seconde)...");
            viewModel.incrementFromBackground();
        });

        // Copie de la commande ADB de test
        binding.btnCopyAdb.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("ADB Command", binding.tvAdbCommand.getText());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Commande ADB copiée !", Toast.LENGTH_SHORT).show();
            logEvent("Commande ADB copiée dans le presse-papiers");
        });

        // Bouton de nettoyage de console
        binding.btnClearLogs.setOnClickListener(v -> {
            logsBuilder.setLength(0);
            binding.tvConsoleLogs.setText("Console nettoyée.\n");
        });

        // Synchronise l'affichage graphique initial du ToggleGroup et de la page
        if (isViewModelMode) {
            binding.toggleGroupMode.check(R.id.btnModeViewModel);
        } else {
            binding.toggleGroupMode.check(R.id.btnModeClassic);
        }
        setMode(isViewModelMode);
    }

    // Bascule et configure les éléments de l'UI selon le mode sélectionné
    private void setMode(boolean viewModelMode) {
        this.isViewModelMode = viewModelMode;
        if (viewModelMode) {
            binding.tvModeHeader.setText("Mode Actuel : Avec ViewModel");
            binding.tvModeHeader.setTextColor(ContextCompat.getColor(this, R.color.color_success));
            binding.layoutClassicOptions.setVisibility(View.GONE);
            binding.layoutViewModelOptions.setVisibility(View.VISIBLE);

            // Affiche le Hashcode du ViewModel
            String vmHash = Integer.toHexString(System.identityHashCode(viewModel));
            binding.tvViewModelHash.setText("CounterViewModel@" + vmHash);
            binding.tvViewModelHash.setTextColor(ContextCompat.getColor(this, R.color.white));

            // Force la mise à jour graphique avec la valeur du ViewModel
            Integer vmCount = viewModel.getCount().getValue();
            binding.tvCount.setText(String.valueOf(vmCount != null ? vmCount : 0));
            logEvent("Mode basculé : AVEC VIEWMODEL (LiveData + SavedStateHandle)");
        } else {
            binding.tvModeHeader.setText("Mode Actuel : Sans ViewModel");
            binding.tvModeHeader.setTextColor(ContextCompat.getColor(this, R.color.color_warning));
            binding.layoutClassicOptions.setVisibility(View.VISIBLE);
            binding.layoutViewModelOptions.setVisibility(View.GONE);

            binding.tvViewModelHash.setText("Non instancié");
            binding.tvViewModelHash.setTextColor(ContextCompat.getColor(this, R.color.color_warning));

            // Force la mise à jour graphique avec la valeur classique locale
            updateUI();
            logEvent("Mode basculé : SANS VIEWMODEL (Variable d'instance simple)");
        }
    }

    private void updateUI() {
        binding.tvCount.setText(String.valueOf(classicCount));
    }

    // Ajoute un log d'événement dans la console en temps réel et défile vers le bas
    private void logEvent(String eventName) {
        String activityHash = Integer.toHexString(System.identityHashCode(this));
        String logLine = String.format("[Activity@%s] %s\n", activityHash, eventName);
        logsBuilder.append(logLine);
        if (binding != null && binding.tvConsoleLogs != null) {
            binding.tvConsoleLogs.setText(logsBuilder.toString());
            binding.scrollViewConsole.post(() -> binding.scrollViewConsole.fullScroll(View.FOCUS_DOWN));
        }
    }

    // --- Cycles de vie ---

    @Override
    protected void onStart() {
        super.onStart();
        logEvent("onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        logEvent("onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        logEvent("onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        logEvent("onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        logEvent("onDestroy()");
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        logEvent("onRestoreInstanceState()");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        logEvent("onSaveInstanceState()");
        
        // Sauvegarde de l'état global de l'application
        outState.putBoolean("mode_key", isViewModelMode);
        outState.putBoolean("save_state_checked_key", binding.switchSaveState.isChecked());
        outState.putString("logs_key", logsBuilder.toString());

        // En mode classique, on ne sauvegarde le compteur que si l'utilisateur l'a activé
        if (!isViewModelMode) {
            if (binding.switchSaveState.isChecked()) {
                outState.putInt("classic_count_key", classicCount);
                logEvent("onSaveInstanceState() : classicCount=" + classicCount + " sauvegardé");
            } else {
                logEvent("onSaveInstanceState() : classicCount NON sauvegardé (option désactivée)");
            }
        }
    }
}