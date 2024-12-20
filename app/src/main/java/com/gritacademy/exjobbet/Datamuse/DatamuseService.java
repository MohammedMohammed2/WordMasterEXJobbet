package com.gritacademy.exjobbet.Datamuse;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
public interface DatamuseService {

    @GET("words")
    Call<List<DatamuseWord>> getSynonyms(@Query("rel_syn") String word);

    @GET("words")
    Call<List<DatamuseWord>> getAntonyms(@Query("rel_ant") String word);

    @GET("words")
    Call<List<DatamuseWord>> getRhymes(@Query("rel_rhy") String word);

    // Add this new endpoint for example sentences
    @GET("words")
    Call<List<String>> getExamples(@Query("rel_trg") String word);  // `rel_trg` is for example sentences in Datamuse API
}

