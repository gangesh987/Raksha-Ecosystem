package com.rakshacall.safety.presentation.protection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.domain.model.VerificationStatus
import com.rakshacall.safety.presentation.theme.RiskCritical
import com.rakshacall.safety.presentation.theme.RiskLow
import com.rakshacall.safety.presentation.theme.RiskMedium
import com.rakshacall.safety.presentation.theme.Slate500
import com.rakshacall.safety.presentation.theme.TealPrimary

@Composable
fun VerificationCoachScreen(
    onNavigateBack: () -> Unit
) {
    var currentStatus by remember { mutableStateOf(VerificationStatus.IN_PROGRESS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "INDEPENDENT VERIFICATION COACH",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TealPrimary
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Urgency Warning
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = RiskMedium, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Scammers rely on urgency to prevent you from thinking clearly. Pause the interaction now. Real law enforcement will never forbid you from verifying through official channels.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }

            Text(text = "5-Step Independent Verification Protocol", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            VerificationStepCard(
                stepNumber = "1",
                title = "Pause or Disconnect the Call",
                description = "Tell the caller you will call back through their official publicly listed department number. If they threaten immediate arrest if you hang up, this is 100% confirmation of a digital arrest scam."
            )

            VerificationStepCard(
                stepNumber = "2",
                title = "Do NOT Use Caller's Links or Numbers",
                description = "Never call phone numbers, Skype IDs, or follow links provided by the caller. Fraudsters route you to accomplices posing as senior officers."
            )

            VerificationStepCard(
                stepNumber = "3",
                title = "Find Official Information Independently",
                description = "Search the official government portal (e.g. cybercrime.gov.in, state police portal, or official bank helpline). Use another device if you suspect remote access software."
            )

            VerificationStepCard(
                stepNumber = "4",
                title = "Contact Through Independent Channels",
                description = "Dial 1930 (National Cyber Crime Helpline in India) or visit your nearest local police station physically with your identification."
            )

            VerificationStepCard(
                stepNumber = "5",
                title = "Verify With a Trusted Family Member",
                description = "Scammers enforce isolation by saying 'keep this secret'. Break the isolation immediately by speaking with a trusted family member or friend."
            )

            // Status Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Update Verification Status", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    VerificationStatusOption(
                        title = "In Progress (Checking official sources)",
                        selected = currentStatus == VerificationStatus.IN_PROGRESS,
                        onClick = { currentStatus = VerificationStatus.IN_PROGRESS }
                    )
                    VerificationStatusOption(
                        title = "Verified (Legitimate organization confirmed)",
                        selected = currentStatus == VerificationStatus.VERIFIED,
                        onClick = { currentStatus = VerificationStatus.VERIFIED }
                    )
                    VerificationStatusOption(
                        title = "Unable to Verify / Confirmed Scam",
                        selected = currentStatus == VerificationStatus.UNABLE_TO_VERIFY,
                        onClick = { currentStatus = VerificationStatus.UNABLE_TO_VERIFY }
                    )
                }
            }

            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save & Return to Session", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun VerificationStepCard(stepNumber: String, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(TealPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stepNumber, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, fontSize = 12.sp, color = Slate500, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun VerificationStatusOption(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
