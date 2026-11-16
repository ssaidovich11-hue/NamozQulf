package tj.namozqulf

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)
        setContent { NamozScreen() }
    }
}

private val prayerNames = listOf("Бомдод", "Пешин", "Аср", "Шом", "Хуфтан")
private val defaultTimes = listOf("05:00", "12:30", "16:00", "18:30", "20:00")

@Composable
private fun NamozScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PrayerScheduler.PREFS, Context.MODE_PRIVATE) }
    val times = remember {
        mutableStateListOf<String>().apply {
            prayerNames.indices.forEach { index ->
                add(prefs.getString("time_$index", defaultTimes[index]) ?: defaultTimes[index])
            }
        }
    }
    val focusMinutes = remember { mutableIntStateOf(prefs.getInt("focus_minutes", 30)) }
    val savedMessage = remember { androidx.compose.runtime.mutableStateOf("") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("НамозҚулф", style = MaterialTheme.typography.headlineMedium)
                Text("Вақтҳои намозро худатон гузоред. 20 дақиқа пеш барнома режими тамаркузро фаъол мекунад.")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Вақтҳои намоз", style = MaterialTheme.typography.titleLarge)
                        prayerNames.forEachIndexed { index, name ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name, modifier = Modifier.padding(top = 12.dp))
                                OutlinedButton(onClick = {
                                    val parts = times[index].split(":")
                                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
                                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    TimePickerDialog(context, { _, h, m ->
                                        times[index] = "%02d:%02d".format(h, m)
                                    }, hour, minute, true).show()
                                }) { Text(times[index]) }
                            }
                        }
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Тамаркуз", style = MaterialTheme.typography.titleLarge)
                        Text("Пас аз оғози режим телефон барои зангҳои изтирорӣ дастрас мемонад. Ин барнома экран ё ҳамаи барномаҳоро қулф намекунад.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(20, 30, 45).forEach { mins ->
                                if (focusMinutes.intValue == mins) Button(onClick = { focusMinutes.intValue = mins }) { Text("$mins дақ.") }
                                else OutlinedButton(onClick = { focusMinutes.intValue = mins }) { Text("$mins дақ.") }
                            }
                        }
                    }
                }
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    prayerNames.indices.forEach { prefs.edit().putString("time_$it", times[it]).apply() }
                    prefs.edit().putInt("focus_minutes", focusMinutes.intValue).apply()
                    PrayerScheduler.scheduleAll(context)
                    savedMessage.value = "Вақтҳо нигоҳ дошта шуданд. Ёдрасҳо ҳар рӯз аз нав танзим мешаванд."
                }) { Text("Нигоҳ доштан ва гузоштани ёдрасҳо") }
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (!manager.isNotificationPolicyAccessGranted) {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        savedMessage.value = "Дар танзимоти Android ба НамозҚулф иҷозати «Халал нарасонед»-ро диҳед."
                    } else {
                        PrayerScheduler.startFocus(context)
                        savedMessage.value = "Режими тамаркуз фаъол шуд; баъди ${focusMinutes.intValue} дақиқа худкор хомӯш мешавад."
                    }
                }) { Text("Ҳозир режими тамаркузро фаъол кун") }
                if (savedMessage.value.isNotBlank()) Text(savedMessage.value, color = MaterialTheme.colorScheme.primary)
                Text("Эзоҳ: Android метавонад вақти ёдрасро вобаста ба сарфаи батарея каме тағйир диҳад. Иҷозати огоҳиномаҳо ва дастрасии «Халал нарасонед»-ро дар танзимот фаъол кунед.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel("prayer_focus", "Ёдрасҳои намоз", NotificationManager.IMPORTANCE_HIGH)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }
}
