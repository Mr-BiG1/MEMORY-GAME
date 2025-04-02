package com.example.memorygame.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.memorygame.R
import kotlin.random.Random

class GameFragment : Fragment() {

    companion object {
        private const val GRID_SIZE = 36
        private const val EASY_TILES = 4
        private const val HARD_TILES = 5
        private const val EASY_SCORE_INCREMENT = 10
        private const val HARD_SCORE_INCREMENT = 20
        private const val HIGHLIGHT_DURATION = 3000L
        private const val SELECTION_DURATION = 5000L
        private const val ROUND_DELAY = 1000L
        private const val TILE_HEIGHT = 130
        private const val MAX_ROUNDS = 3
    }

    private lateinit var gridLayout: GridLayout
    private lateinit var scoreText: TextView
    private lateinit var timerText: TextView
    private val buttons = mutableListOf<Button>()

    private var highlightedIndices = mutableSetOf<Int>()
    private var currentRound = 1
    private var score = 0
    private var tilesToRemember = EASY_TILES
    private var correctSelections = 0
    private var isRoundActive = false

    private var highlightTimer: CountDownTimer? = null
    private var selectionTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_game, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gridLayout = view.findViewById(R.id.grid_layout)
        scoreText = view.findViewById(R.id.tv_score)
        timerText = view.findViewById(R.id.tv_timer)

        tilesToRemember = if (loadDifficulty() == "hard") HARD_TILES else EASY_TILES
        scoreText.text = getString(R.string.score_format, score)
        timerText.text = ""

        generateGrid()
        startRound()
    }

    override fun onDestroyView() {
        highlightTimer?.cancel()
        selectionTimer?.cancel()
        super.onDestroyView()
    }

    private fun loadDifficulty(): String {
        val prefs = requireActivity().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        return prefs.getString("difficulty", "easy") ?: "easy"
    }

    private fun generateGrid() {
        gridLayout.removeAllViews()
        buttons.clear()

        for (i in 0 until GRID_SIZE) {
            val button = Button(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
                text = ""
                tag = "unselected"
                setBackgroundColor(Color.LTGRAY)
                contentDescription = getString(R.string.tile_description, i + 1)
                setOnClickListener { onTileClick(i) }
                isEnabled = false
            }
            buttons.add(button)
            gridLayout.addView(button)
        }
    }

    private fun startRound() {
        isRoundActive = false
        highlightedIndices.clear()
        correctSelections = 0

        buttons.forEach {
            it.setBackgroundColor(Color.LTGRAY)
            it.tag = "unselected"
            it.isEnabled = false
        }

        // Generate unique random indices
        val random = Random(System.currentTimeMillis())
        while (highlightedIndices.size < tilesToRemember) {
            val randomIndex = random.nextInt(GRID_SIZE)
            highlightedIndices.add(randomIndex)
        }

        timerText.text = "Memorize the tiles!"

        // Highlight the tiles
        highlightedIndices.forEach { index ->
            buttons[index].setBackgroundColor(Color.YELLOW)
        }

        highlightTimer = object : CountDownTimer(HIGHLIGHT_DURATION, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                timerText.text = "Memorize: ${secondsLeft}s"
            }

            override fun onFinish() {
                hideTiles()
                enableTileSelection()
                startSelectionTimer()
            }
        }.start()
    }

    private fun hideTiles() {
        highlightedIndices.forEach { index ->
            buttons[index].setBackgroundColor(Color.LTGRAY)
        }
    }

    private fun enableTileSelection() {
        buttons.forEach {
            it.isEnabled = true
        }
        isRoundActive = true
    }

    private fun startSelectionTimer() {
        selectionTimer = object : CountDownTimer(SELECTION_DURATION, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                timerText.text = "Time Left: ${secondsLeft}s"
            }

            override fun onFinish() {
                timerText.text = "Time Left: 0s"
                endGame(getString(R.string.time_up_message))
            }
        }.start()
    }

    private fun onTileClick(index: Int) {
        if (!isRoundActive) return

        if (!highlightedIndices.contains(index)) {
            buttons[index].setBackgroundColor(Color.RED)
            selectionTimer?.cancel()
            timerText.text = ""
            endGame(getString(R.string.wrong_tile_message))
            return
        }

        if (buttons[index].tag == "selected") return

        buttons[index].setBackgroundColor(Color.GREEN)
        buttons[index].tag = "selected"
        buttons[index].isEnabled = false
        correctSelections++

        if (correctSelections == tilesToRemember) {
            selectionTimer?.cancel()
            timerText.text = ""

            score += if (tilesToRemember == EASY_TILES) EASY_SCORE_INCREMENT else HARD_SCORE_INCREMENT
            scoreText.text = getString(R.string.score_format, score)

            if (currentRound == MAX_ROUNDS && tilesToRemember == EASY_TILES) {
                tilesToRemember = HARD_TILES
                currentRound = 1 // Reset round counter when switching to hard mode
            } else {
                currentRound++
            }

            Toast.makeText(requireContext(), getString(R.string.correct_message), Toast.LENGTH_SHORT).show()

            view?.postDelayed({
                resetTilesForNextRound()
                startRound()
            }, ROUND_DELAY)
        }
    }

    private fun resetTilesForNextRound() {
        buttons.forEach {
            it.setBackgroundColor(Color.LTGRAY)
            it.tag = "unselected"
            it.isEnabled = false
        }
    }

    private fun endGame(message: String) {
        isRoundActive = false
        highlightTimer?.cancel()
        selectionTimer?.cancel()
        timerText.text = ""

        buttons.forEach { it.isEnabled = false }

        // Show all correct tiles at game end
        highlightedIndices.forEach { index ->
            if (buttons[index].tag != "selected") {
                buttons[index].setBackgroundColor(Color.YELLOW)
            }
        }

        saveHighScore()
        showGameOverDialog(message)
    }

    private fun saveHighScore() {
        val prefs = requireActivity().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        val playerName = prefs.getString("player_name", null) ?: return

        val oldScores = prefs.getStringSet("high_scores", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        oldScores.add("$playerName:$score")

        val sortedScores = oldScores.mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2) {
                val name = parts[0]
                val scoreValue = parts[1].toIntOrNull()
                if (scoreValue != null) Pair(name, scoreValue) else null
            } else null
        }.sortedWith(
            compareByDescending<Pair<String, Int>> { it.second }
                .thenBy { it.first }
        ).take(3)
            .map { "${it.first}:${it.second}" }
            .toSet()

        prefs.edit().putStringSet("high_scores", sortedScores).apply()
    }

    private fun showGameOverDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.game_over_title))
            .setMessage("$message\n${getString(R.string.final_score_format, score)}\n\n${getString(R.string.play_again_prompt)}")
            .setPositiveButton(getString(R.string.yes)) { _, _ -> restartGame() }
            .setNegativeButton(getString(R.string.no)) { _, _ ->
                findNavController().navigate(R.id.nav_welcome)
            }
            .setCancelable(false)
            .show()
    }

    private fun restartGame() {
        score = 0
        currentRound = 1
        tilesToRemember = if (loadDifficulty() == "hard") HARD_TILES else EASY_TILES
        scoreText.text = getString(R.string.score_format, score)
        timerText.text = ""
        buttons.forEach {
            it.setBackgroundColor(Color.LTGRAY)
            it.tag = "unselected"
            it.isEnabled = false
        }
        startRound()
    }
}