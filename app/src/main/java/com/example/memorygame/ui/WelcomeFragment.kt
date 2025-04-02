package com.example.memorygame.ui

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.fragment.findNavController
import com.example.memorygame.R

class WelcomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameInput = view.findViewById<EditText>(R.id.et_player_name)
        val startButton = view.findViewById<Button>(R.id.btn_start_game)
        val difficultyGroup = view.findViewById<RadioGroup>(R.id.difficulty_group)

        startButton.setOnClickListener {
            val playerName = nameInput.text.toString().trim()

            val sharedPref = activity?.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
            val editor = sharedPref?.edit()

            if (playerName.isNotEmpty()) {
                editor?.putString("player_name", playerName)
            }

            // Save difficulty
            val difficulty = when (difficultyGroup.checkedRadioButtonId) {
                R.id.radio_hard -> "hard"
                else -> "easy"
            }
            editor?.putString("difficulty", difficulty)

            editor?.apply()

            // Navigate to Game screen
            findNavController().navigate(R.id.nav_game)
        }
    }
}
