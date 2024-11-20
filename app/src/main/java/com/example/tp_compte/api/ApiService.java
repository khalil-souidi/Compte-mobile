package com.example.tp_compte.api;

import com.example.tp_compte.beans.Compte;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {
    @GET("banque/comptes")
    Call<List<Compte>> getAllComptes();

    @GET("banque/comptes/{id}")
    Call<Compte> getCompteById(@Path("id") Long id);

    @PUT("banque/comptes/{id}")
    Call<Compte> updateCompte(@Path("id") Long id, @Body Compte compte);

    @POST("banque/comptes")
    Call<Compte> createCompte(@Body Compte compte);

    @DELETE("banque/comptes/{id}")
    Call<Void> deleteCompte(@Path("id") Long id);
}
