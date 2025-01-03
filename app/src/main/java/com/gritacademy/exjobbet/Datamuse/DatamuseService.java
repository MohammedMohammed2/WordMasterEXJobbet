package com.gritacademy.exjobbet.Datamuse;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
public interface DatamuseService {

    @GET("words")
    Call<List<DatamuseWord>> getSynonyms(@Query("rel_syn") String word);

}

