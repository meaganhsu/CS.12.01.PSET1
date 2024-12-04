package com.example.cs1201pset;

import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.*;

import java.util.*;

public class StartingScreen extends AppCompatActivity {

    private Button analyseButton;
    private TextView title;
    private Spinner dropdown;
    private CheckBox checkBox;
    private static String fileName;
    private static boolean excludeCW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_starting_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // initialising components in the start screen
        analyseButton = findViewById(R.id.analyseButton);
        title = findViewById(R.id.title);
        checkBox = findViewById(R.id.checkBox);
        dropdown = findViewById(R.id.dropdown);
        excludeCW = false;

        // creating checkbox: www.youtube.com/watch?v=RIHTYPRRvyc
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                // if checkbox is ticked
                excludeCW = true;
            }
        });

        // creating spinner: www.youtube.com/watch?v=4ogzfAipGS8
        ArrayList<String> files = new ArrayList(Arrays.asList(new String[] {"1984.txt", "AnimalFarm.txt", "Lottery.pdf", "TextOne.txt", "TextTwo.txt", "StoryOfAnHour.pdf"}));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, files);
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        dropdown.setAdapter(adapter);
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                fileName = adapterView.getSelectedItem().toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // analyse button
        analyseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StartingScreen.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    public static String getFileName() {
        return fileName;
    }

    public static boolean getExcludeCW() {
        return excludeCW;
    }
}