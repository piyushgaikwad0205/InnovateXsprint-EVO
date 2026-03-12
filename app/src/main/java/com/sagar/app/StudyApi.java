package com.sagar.app;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface StudyApi {
    @POST("generate-study-plan")
    Call<StudyResponse> generateStudyPlan(@Body Map<String, Object> body);
}
