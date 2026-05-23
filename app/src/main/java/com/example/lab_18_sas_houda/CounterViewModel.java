package com.example.lab_18_sas_houda;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

public class CounterViewModel extends ViewModel {

    private static final String COUNT_KEY = "count_key";
    private final SavedStateHandle savedStateHandle;
    
    // MutableLiveData exposée en interne
    private final MutableLiveData<Integer> countLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public CounterViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
        // Restauration depuis SavedStateHandle si existante, sinon initialisation à 0
        Integer initialCount = savedStateHandle.get(COUNT_KEY);
        if (initialCount == null) {
            initialCount = 0;
        }
        countLiveData.setValue(initialCount);
    }

    // Incrémente la valeur
    public void increment() {
        Integer current = countLiveData.getValue();
        if (current != null) {
            updateValue(current + 1);
        }
    }

    // Décrémente la valeur
    public void decrement() {
        Integer current = countLiveData.getValue();
        if (current != null) {
            updateValue(current - 1);
        }
    }

    // Réinitialise la valeur
    public void reset() {
        updateValue(0);
    }

    // Bonus 1: postValue depuis un thread background
    public void incrementFromBackground() {
        isLoading.postValue(true);
        new Thread(() -> {
            try {
                // Simule une tâche d'arrière-plan de 1 seconde (ex: appel API)
                Thread.sleep(1000);
                Integer current = countLiveData.getValue();
                if (current != null) {
                    // postValue est safe depuis n'importe quel thread et repasse sur le thread principal
                    int newValue = current + 1;
                    savedStateHandle.set(COUNT_KEY, newValue);
                    countLiveData.postValue(newValue);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    // Met à jour la valeur et met à jour le SavedStateHandle pour la persistance face au process death
    private void updateValue(int newValue) {
        savedStateHandle.set(COUNT_KEY, newValue);
        countLiveData.setValue(newValue);
    }

    // Getter exposé à l'Activity (lecture seule = bonne pratique)
    public LiveData<Integer> getCount() {
        return countLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
}
