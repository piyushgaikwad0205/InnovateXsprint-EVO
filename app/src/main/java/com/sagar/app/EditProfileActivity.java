package com.sagar.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class EditProfileActivity extends AppCompatActivity {
    private android.widget.EditText etFullName, etDob, etEmail, etPhone;
    private android.widget.RadioGroup rgGender;
    private com.google.firebase.auth.FirebaseAuth mAuth;
    private com.google.firebase.firestore.FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        etFullName = findViewById(R.id.etFullName);
        etDob = findViewById(R.id.etDob);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        rgGender = findViewById(R.id.rgGender);

        loadUserData();

        findViewById(R.id.btnUpdateProfile).setOnClickListener(v -> updateProfile());

        // Date Picker
        etDob.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        final java.util.Calendar c = java.util.Calendar.getInstance();
        int year = c.get(java.util.Calendar.YEAR) - 18;
        int month = c.get(java.util.Calendar.MONTH);
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> etDob
                        .setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1),
                year, month, day);
        datePickerDialog.show();
    }

    private void loadUserData() {
        com.google.firebase.auth.FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            etEmail.setText(user.getEmail());
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                etFullName.setText(user.getDisplayName());
            }

            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            if (etFullName.getText().toString().isEmpty()) {
                                etFullName.setText(documentSnapshot.getString("name"));
                            }
                            etDob.setText(documentSnapshot.getString("dob"));
                            etPhone.setText(documentSnapshot.getString("phone"));

                            String gender = documentSnapshot.getString("gender");
                            if ("Male".equals(gender)) {
                                rgGender.check(R.id.rbMale);
                            } else if ("Female".equals(gender)) {
                                rgGender.check(R.id.rbFemale);
                            } else if ("Other".equals(gender)) {
                                rgGender.check(R.id.rbOther);
                            }
                        }
                    });
        }
    }

    private void updateProfile() {
        String name = etFullName.getText().toString().trim();
        String dob = etDob.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        int selectedId = rgGender.getCheckedRadioButtonId();
        if (selectedId == -1) {
            android.widget.Toast.makeText(this, "Please select your gender", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        android.widget.RadioButton rbSelected = findViewById(selectedId);
        String gender = rbSelected.getText().toString();

        if (name.isEmpty() || dob.isEmpty() || phone.isEmpty()) {
            android.widget.Toast.makeText(this, "All fields are required", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.firebase.auth.FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Update Auth Profile
            com.google.firebase.auth.UserProfileChangeRequest profileUpdates = new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();
            user.updateProfile(profileUpdates);

            // Update Firestore
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("name", name);
            updates.put("dob", dob);
            updates.put("phone", phone);
            updates.put("gender", gender);
            updates.put("profileComplete", true);

            db.collection("users").document(user.getUid())
                    .set(updates, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        android.widget.Toast.makeText(this, "Profile Updated", android.widget.Toast.LENGTH_SHORT)
                                .show();
                        // Redirect to ProfileActivity for better flow
                        android.content.Intent intent = new android.content.Intent(this, ProfileActivity.class);
                        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        android.widget.Toast.makeText(this, "Update failed", android.widget.Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
