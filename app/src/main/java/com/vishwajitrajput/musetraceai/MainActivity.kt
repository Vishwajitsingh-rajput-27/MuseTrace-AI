package com.vishwajitrajput.musetraceai

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import android.view.WindowManager
import com.vishwajitrajput.musetraceai.databinding.ActivityMainBinding
import com.vishwajitrajput.musetraceai.domain.SettingsRepository
import com.vishwajitrajput.musetraceai.domain.model.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHost.navController
        appBarConfiguration = AppBarConfiguration(setOf(R.id.onboardingFragment, R.id.homeFragment))
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        observeRuntimeSettings()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        return navHost.navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun observeRuntimeSettings() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.settings.collect { settings ->
                    AppCompatDelegate.setDefaultNightMode(
                        when (settings.appTheme) {
                            AppTheme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            AppTheme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                            AppTheme.Light -> AppCompatDelegate.MODE_NIGHT_NO
                        },
                    )
                    if (settings.keepScreenAwake) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }
    }
}
