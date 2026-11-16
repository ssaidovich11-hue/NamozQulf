package tj.namozqulf

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Forest = Color(0xFF123D32)
private val Green = Color(0xFF1F6B51)
private val Mint = Color(0xFFDCEFE4)
private val Cream = Color(0xFFF6F7F1)
private val Ink = Color(0xFF172A24)
private val Muted = Color(0xFF718078)

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
    val message = remember { mutableStateOf("") }
    val saved = remember { mutableStateOf(false) }
    val next = nextPrayer(times)

    MaterialTheme(colorScheme = lightColorScheme(primary = Green, onPrimary = Color.White, background = Cream, surface = Color.White, onSurface = Ink)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                HeaderCard(next.first, next.second)

                SectionTitle("Вақтҳои намоз", "Вақтҳоро бо пахш кардани соат иваз кунед")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                        prayerNames.forEachIndexed { index, name ->
                            PrayerTimeRow(name = name, time = times[index], isNext = index == next.third) {
                                val parts = times[index].split(":")
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        times[index] = "%02d:%02d".format(hour, minute)
                                        saved.value = false
                                        message.value = ""
                                    },
                                    parts.getOrNull(0)?.toIntOrNull() ?: 12,
                                    parts.getOrNull(1)?.toIntOrNull() ?: 0,
                                    true
                                ).show()
                            }
                            if (index != prayerNames.lastIndex) androidx.compose.material3.HorizontalDivider(color = Color(0xFFF0F2EE))
                        }
                    }
                }

                SectionTitle("Тамаркуз", "Барои вақти ор��мии худ давомнокиро интихоб кунед")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).background(Mint, CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.SelfImprovement, contentDescription = null, tint = Green) }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Режими оромӣ", fontWeight = FontWeight.SemiBold, color = Ink)
                                Text("Баъди оғози намоз худкор анҷом меёбад", fontSize = 12.sp, color = Muted)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(20, 30, 45).forEach { minutes ->
                                if (focusMinutes.intValue == minutes) {
                                    Button(
                                        onClick = { focusMinutes.intValue = minutes; saved.value = false },
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = ButtonDefaults.ContentPadding
                                    ) { Text("$minutes дақ.") }
                                } else {
                                    OutlinedButton(
                                        onClick = { focusMinutes.intValue = minutes; saved.value = false },
                                        shape = RoundedCornerShape(14.dp)
                                    ) { Text("$minutes дақ.", color = Green) }
                                }
                            }
                        }
                        Text(
                            "Android танҳо огоҳиномаҳо ва садоҳоро маҳдуд мекунад; телефон пурра қулф намешавад.",
                            color = Muted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        prayerNames.indices.forEach { prefs.edit().putString("time_$it", times[it]).apply() }
                        prefs.edit().putInt("focus_minutes", focusMinutes.intValue).apply()
                        PrayerScheduler.scheduleAll(context)
                        saved.value = true
                        message.value = "Вақтҳо нигоҳ дошта шуданд ва ёдрасҳои ҳаррӯза танзим шуданд."
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(if (saved.value) Icons.Filled.Check else Icons.Filled.Schedule, contentDescription = null)
                    Spacer(Modifier.width(9.dp))
                    Text(if (saved.value) "Вақтҳо нигоҳ дошта шуданд" else "Нигоҳ доштан ва фаъол кардан", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        if (!manager.isNotificationPolicyAccessGranted) {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            message.value = "Дар танзимоти Android ба НамозҚулф иҷозати «Халал нарасонед»-ро диҳед."
                        } else {
                            PrayerScheduler.startFocus(context)
                            message.value = "Режими оромӣ барои ${focusMinutes.intValue} дақиқа фаъол шуд."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                    Spacer(Modifier.width(9.dp))
                    Text("Ҳозир режими оромиро санҷидан")
                }

                if (message.value.isNotBlank()) {
                    Text(message.value, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), color = Green, fontSize = 13.sp)
                }
                Text(
                    "Ёдрас 20 дақиқа пеш аз вақти интихобшуда меояд. Барои огоҳиномаҳо ва режими «Халал нарасонед» иҷозат диҳед.",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun HeaderCard(nextName: String, nextTime: String) {
    val date = remember { SimpleDateFormat("EEEE, d MMMM", Locale("tg")).format(Date()) }
    Box(
        modifier = Modifier.fillMaxWidth().background(
            Brush.linearGradient(listOf(Forest, Green)),
            RoundedCornerShape(28.dp)
        ).padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("НАМОЗҚУЛФ", color = Color(0xFFBFE3CF), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Оромии дил,\nтамаркузи бештар", color = Color.White, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.13f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.SelfImprovement, contentDescription = null, tint = Color.White, modifier = Modifier.size(27.dp))
                }
            }
            Text(date.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("tg")) else it.toString() }, color = Color(0xFFD0E4D9), fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp)).padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("НАМОЗИ НАЗДИК", color = Color(0xFFC6E1D2), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(nextName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccessTime, contentDescription = null, tint = Color(0xFFC6E1D2), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(nextTime, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(subtitle, fontSize = 12.sp, color = Muted)
    }
}

@Composable
private fun PrayerTimeRow(name: String, time: String, isNext: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).background(if (isNext) Mint else Color(0xFFF3F5F1), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Schedule, contentDescription = null, tint = if (isNext) Green else Muted, modifier = Modifier.size(19.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name, color = Ink, fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Medium)
                if (isNext) Text("Наздиктарин", color = Green, fontSize = 11.sp)
            }
        }
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(12.dp)) {
            Text(time, color = Green, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun nextPrayer(times: List<String>): Triple<String, String, Int> {
    val now = Calendar.getInstance()
    val next = prayerNames.indices.mapNotNull { index ->
        val parts = times[index].split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis to index
    }.minByOrNull { it.first } ?: (System.currentTimeMillis() to 0)
    return Triple(prayerNames[next.second], times[next.second], next.second)
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel("prayer_focus", "Ёдрасҳои намоз", NotificationManager.IMPORTANCE_HIGH)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }
}
