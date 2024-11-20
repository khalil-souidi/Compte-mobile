package com.example.tp_compte

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tp_compte.beans.Compte
import com.example.tp_compte.beans.TypeCompte
import com.example.tp_compte.Config.RetrofitClient
import com.example.tp_compte.api.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompteApp()
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompteApp() {
    var comptes by remember { mutableStateOf(listOf<Compte>()) }
    val apiService = RetrofitClient.getJsonApiService()

    var showDialog by remember { mutableStateOf(false) }
    var selectedCompte by remember { mutableStateOf<Compte?>(null) }

    // Fetch data from API on launch
    LaunchedEffect(Unit) {
        fetchComptes(apiService) { fetchedComptes ->
            comptes = fetchedComptes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Liste des Comptes") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                showDialog = true
                selectedCompte = null
            }) {
                Text("+")
            }
        },
        content = {
            CompteList(
                comptes = comptes,
                onDelete = { compteToDelete ->
                    deleteCompte(apiService, compteToDelete, comptes) { updatedComptes ->
                        comptes = updatedComptes
                    }
                },
                onEdit = { compteToEdit ->
                    selectedCompte = compteToEdit
                    showDialog = true
                }
            )
        }
    )

    if (showDialog) {
        AccountDialog(
            compte = selectedCompte,
            onSave = { solde, type ->
                if (selectedCompte == null) {
                    // Add new account
                    addCompte(apiService, comptes, solde, type) { updatedComptes ->
                        comptes = updatedComptes
                    }
                } else {
                    // Update existing account
                    updateCompte(apiService, selectedCompte!!, comptes, solde, type) { updatedComptes ->
                        comptes = updatedComptes
                    }
                }
                showDialog = false
            },
            onCancel = {
                showDialog = false
            }
        )
    }
}

@Composable
fun CompteList(
    comptes: List<Compte>,
    onDelete: (Compte) -> Unit,
    onEdit: (Compte) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(comptes) { compte ->
            CompteCard(compte, onDelete, onEdit)
        }
    }
}

@Composable
fun CompteCard(
    compte: Compte,
    onDelete: (Compte) -> Unit,
    onEdit: (Compte) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Solde: ${compte.solde}")
            Text(text = "Type: ${compte.type.name}")
            Text(text = "Date de création: ${compte.dateCreation ?: "N/A"}")

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(onClick = { onDelete(compte) }) {
                    Text("Supprimer")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onEdit(compte) }) {
                    Text("Modifier")
                }
            }
        }
    }
}

@Composable
fun AccountDialog(
    compte: Compte?,
    onSave: (Double, TypeCompte) -> Unit,
    onCancel: () -> Unit
) {
    var solde by remember { mutableStateOf(compte?.solde?.toString() ?: "") }
    var type by remember { mutableStateOf(compte?.type ?: TypeCompte.COURANT) }

    AlertDialog(
        onDismissRequest = { onCancel() },
        title = { Text(if (compte == null) "Ajouter un compte" else "Modifier le compte") },
        text = {
            Column {
                OutlinedTextField(
                    value = solde,
                    onValueChange = { solde = it },
                    label = { Text("Solde") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Type de compte:")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = type == TypeCompte.COURANT,
                        onClick = { type = TypeCompte.COURANT }
                    )
                    Text("Courant")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = type == TypeCompte.EPARGNE,
                        onClick = { type = TypeCompte.EPARGNE }
                    )
                    Text("Épargne")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val soldeValue = solde.toDoubleOrNull()
                    if (soldeValue != null) {
                        onSave(soldeValue, type)
                    }
                }
            ) {
                Text(if (compte == null) "Ajouter" else "Modifier")
            }
        },
        dismissButton = {
            Button(onClick = { onCancel() }) {
                Text("Annuler")
            }
        }
    )
}

private fun fetchComptes(
    apiService: ApiService,
    onResult: (List<Compte>) -> Unit
) {
    apiService.getAllComptes().enqueue(object : Callback<List<Compte>> {
        override fun onResponse(call: Call<List<Compte>>, response: Response<List<Compte>>) {
            if (response.isSuccessful) {
                onResult(response.body() ?: emptyList())
                Log.d("CompteApp", "Fetched comptes: ${response.body()}")
            } else {
                Log.e("CompteApp", "Failed to fetch comptes: ${response.code()} - ${response.message()}")
            }
        }

        override fun onFailure(call: Call<List<Compte>>, t: Throwable) {
            Log.e("CompteApp", "Network error during fetch", t)
        }
    })
}

private fun addCompte(
    apiService: ApiService,
    currentComptes: List<Compte>,
    solde: Double,
    type: TypeCompte,
    onResult: (List<Compte>) -> Unit
) {
    // Get the current date
    val currentDate = java.util.Date()

    val newCompte = Compte(null, solde, currentDate, type) // Include the current date
    apiService.createCompte(newCompte).enqueue(object : Callback<Compte> {
        override fun onResponse(call: Call<Compte>, response: Response<Compte>) {
            if (response.isSuccessful) {
                val addedCompte = response.body()
                if (addedCompte != null) {
                    onResult(currentComptes + addedCompte)
                    Log.d("CompteApp", "Added compte: $addedCompte")
                }
            } else {
                Log.e("CompteApp", "Failed to add compte: ${response.code()} - ${response.message()}")
            }
        }

        override fun onFailure(call: Call<Compte>, t: Throwable) {
            Log.e("CompteApp", "Network error during add", t)
        }
    })
}


private fun updateCompte(
    apiService: ApiService,
    compte: Compte,
    currentComptes: List<Compte>,
    solde: Double,
    type: TypeCompte,
    onResult: (List<Compte>) -> Unit
) {
    val updatedCompte = Compte(compte.id, solde, compte.dateCreation, type)
    apiService.updateCompte(compte.id!!, updatedCompte).enqueue(object : Callback<Compte> {
        override fun onResponse(call: Call<Compte>, response: Response<Compte>) {
            if (response.isSuccessful) {
                val newComptes = currentComptes.map {
                    if (it.id == compte.id) response.body() ?: it else it
                }
                onResult(newComptes)
                Log.d("CompteApp", "Updated compte: $updatedCompte")
            } else {
                Log.e("CompteApp", "Failed to update compte: ${response.code()} - ${response.message()}")
            }
        }

        override fun onFailure(call: Call<Compte>, t: Throwable) {
            Log.e("CompteApp", "Network error during update", t)
        }
    })
}

private fun deleteCompte(
    apiService: ApiService,
    compteToDelete: Compte,
    currentComptes: List<Compte>,
    onResult: (List<Compte>) -> Unit
) {
    if (compteToDelete.id == null) {
        Log.e("CompteApp", "Cannot delete compte with null ID")
        return
    }

    apiService.deleteCompte(compteToDelete.id).enqueue(object : Callback<Void> {
        override fun onResponse(call: Call<Void>, response: Response<Void>) {
            if (response.isSuccessful) {
                onResult(currentComptes.filter { it.id != compteToDelete.id })
                Log.d("CompteApp", "Deleted compte: $compteToDelete")
            } else {
                Log.e("CompteApp", "Failed to delete compte: ${response.code()} - ${response.message()}")
            }
        }

        override fun onFailure(call: Call<Void>, t: Throwable) {
            Log.e("CompteApp", "Network error during delete", t)
        }
    })
}
