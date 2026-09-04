package com.example.landguard.ui.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Handles phone verification and initial zone selection.
 * TODO: inject an AuthRepository (OTP request/verify) once the backend auth contract is fixed.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel()