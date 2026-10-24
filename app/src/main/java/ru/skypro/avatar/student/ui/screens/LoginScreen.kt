package ru.skypro.avatar.student.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.skypro.avatar.student.R
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import ru.skypro.avatar.student.core.auth.AuthState
import ru.skypro.avatar.student.core.auth.LoginValidator
import ru.skypro.avatar.student.ui.components.FloatingShapes

@Composable
fun LoginScreen(
    state: AuthState,
    notice: String? = null,
    onLogin: (String, String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var hidden by remember { mutableStateOf(true) }
    val emailValue = email.trim()
    val passwordValue = password.trim()
    val emailErr = LoginValidator.emailError(emailValue).takeIf { email.isNotEmpty() }
    val passErr = LoginValidator.passwordError(passwordValue).takeIf { password.isNotEmpty() }
    val canSubmit = state != AuthState.Loading && emailErr == null && passErr == null
        && emailValue.isNotEmpty() && passwordValue.isNotEmpty()
    val submit = { if (canSubmit) onLogin(emailValue, passwordValue) }

    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.primary),
    ) {
        FloatingShapes(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ava),
                contentDescription = null,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .border(3.dp, colors.onPrimary, CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Вход в Avatar",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.onPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Почта и пароль от учебного кабинета",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = colors.surface.copy(alpha = 0.88f),
            ) {
                Column(Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Почта") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        isError = emailErr != null,
                        supportingText = { emailErr?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { hidden = !hidden }) {
                                Icon(
                                    if (hidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (hidden) "Показать пароль" else "Скрыть пароль",
                                )
                            }
                        },
                        isError = passErr != null,
                        supportingText = { passErr?.let { Text(it) } },
                        visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                    )

                    if (notice != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(notice, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    }
                    if (state == AuthState.NeedLogin && notice == null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Не получилось войти, проверь почту и пароль",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (state == AuthState.NetworkError && notice == null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Нет связи с сервером, попробуй позже",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    FilledTonalButton(
                        onClick = { submit() },
                        enabled = canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (state == AuthState.Loading) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = colors.onSecondaryContainer)
                        } else {
                            Text("Продолжить", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.size(8.dp))
                            Icon(Icons.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}
