package com.propentatech.waka.data.backup

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.propentatech.waka.data.local.entity.Contribution
import com.propentatech.waka.data.local.entity.ProjectItem
import com.propentatech.waka.data.local.entity.Reminder
import com.propentatech.waka.data.repository.ProjectRepository
import com.propentatech.waka.data.repository.ReminderRepository
import com.propentatech.waka.model.Currency
import com.propentatech.waka.model.RepeatType
import com.propentatech.waka.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImportResult(val projectCount: Int, val objectiveCount: Int)

private const val BACKUP_DIR_NAME = "waka"

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class BackupRepository(
    private val projectRepository: ProjectRepository,
    private val reminderRepository: ReminderRepository,
    private val appContext: Context,
) {

    /** Vrai si l'app peut écrire un dossier visible à la racine du stockage partagé. */
    fun hasFullStorageAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Avant Android 11, l'écriture hors sandbox ne dépend que du permission runtime déjà accordée à l'install.
        }

    /**
     * `/storage/emulated/0/waka/`, visible dans n'importe quel gestionnaire de fichiers, quand la
     * permission « tous les fichiers » est accordée ; sinon un dossier privé à l'app, moins visible
     * mais qui ne nécessite jamais de permission.
     */
    fun backupDirectory(): File {
        val dir = if (hasFullStorageAccess()) {
            File(Environment.getExternalStorageDirectory(), BACKUP_DIR_NAME)
        } else {
            File(appContext.getExternalFilesDir(null), BACKUP_DIR_NAME)
        }
        dir.mkdirs()
        return dir
    }

    suspend fun exportToFile(): File = withContext(Dispatchers.IO) {
        val items = projectRepository.getAllItems()
        val contributions = projectRepository.getAllContributions()
        val dependencies = projectRepository.getAllDependencies()
        val reminders = reminderRepository.getAll()

        val backup = WakaBackup(
            projectItems = items.map {
                ProjectItemDto(
                    localId = it.id,
                    parentLocalId = it.parentId,
                    title = it.title,
                    description = it.description,
                    targetAmount = it.targetAmount,
                    currency = it.currency?.name,
                    isManuallyCompleted = it.isManuallyCompleted,
                    isPrivate = it.isPrivate,
                    orderIndex = it.orderIndex,
                    createdAt = it.createdAt,
                    deadlineAt = it.deadlineAt,
                )
            },
            contributions = contributions.map {
                ContributionDto(
                    projectItemLocalId = it.projectItemId,
                    amount = it.amount,
                    currency = it.currency.name,
                    date = it.date,
                    note = it.note,
                )
            },
            dependencies = dependencies.map { TaskDependencyDto(it.taskId, it.dependsOnTaskId) },
            reminders = reminders.map {
                ReminderDto(
                    projectItemLocalId = it.projectItemId,
                    triggerAt = it.triggerAt,
                    message = it.message,
                    isActive = it.isActive,
                    repeatType = it.repeatType.name,
                )
            },
        )

        val fileName = "waka-backup-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date(backup.exportedAt))}.json"
        val file = File(backupDirectory(), fileName)
        file.writeText(json.encodeToString(WakaBackup.serializer(), backup))
        file
    }

    private fun itemKey(parentId: Long?, title: String, targetAmount: Double?, currency: Currency?, deadlineAt: Long?) =
        "$parentId|$title|$targetAmount|${currency?.name}|$deadlineAt"

    private fun contributionKey(projectItemId: Long, amount: Double, currency: String, date: Long, note: String?) =
        "$projectItemId|$amount|$currency|$date|$note"

    private fun reminderKey(projectItemId: Long, triggerAt: Long, message: String, repeatType: String) =
        "$projectItemId|$triggerAt|$message|$repeatType"

    /**
     * Fusionne le fichier avec ce qui existe déjà : un projet ou objectif dont le parent, le titre,
     * le montant cible, la devise et l'échéance correspondent exactement à un élément déjà présent
     * est réutilisé (son id sert de référence pour les versements/dépendances/rappels du fichier) au
     * lieu d'être recréé, donc réimporter deux fois la même sauvegarde ne duplique rien. Seuls les
     * éléments réellement nouveaux sont insérés. Les éléments dont le parent référencé est introuvable
     * (fichier corrompu) sont ignorés plutôt que de faire échouer tout l'import.
     */
    suspend fun importFromUri(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val text = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalArgumentException("Impossible de lire le fichier sélectionné.")
        val backup = json.decodeFromString(WakaBackup.serializer(), text)

        val existingItemIdByKey = projectRepository.getAllItems().associateTo(mutableMapOf()) {
            itemKey(it.parentId, it.title, it.targetAmount, it.currency, it.deadlineAt) to it.id
        }

        val idMap = mutableMapOf<Long, Long>()
        var addedProjects = 0
        var addedObjectives = 0
        val pending = backup.projectItems.toMutableList()
        var progressed = true
        while (pending.isNotEmpty() && progressed) {
            progressed = false
            val iterator = pending.iterator()
            while (iterator.hasNext()) {
                val dto = iterator.next()
                val newParentId = when {
                    dto.parentLocalId == null -> null
                    idMap.containsKey(dto.parentLocalId) -> idMap.getValue(dto.parentLocalId)
                    else -> continue
                }
                val currency = dto.currency?.let { runCatching { Currency.valueOf(it) }.getOrNull() }
                val key = itemKey(newParentId, dto.title, dto.targetAmount, currency, dto.deadlineAt)
                val existingId = existingItemIdByKey[key]
                val newId = existingId ?: projectRepository.createItem(
                    ProjectItem(
                        parentId = newParentId,
                        title = dto.title,
                        description = dto.description,
                        targetAmount = dto.targetAmount,
                        currency = currency,
                        isManuallyCompleted = dto.isManuallyCompleted,
                        isPrivate = dto.isPrivate,
                        orderIndex = dto.orderIndex,
                        deadlineAt = dto.deadlineAt,
                    ),
                ).also {
                    existingItemIdByKey[key] = it
                    if (dto.parentLocalId == null) addedProjects++ else addedObjectives++
                }
                idMap[dto.localId] = newId
                iterator.remove()
                progressed = true
            }
        }

        val existingContributionKeys = projectRepository.getAllContributions().mapTo(mutableSetOf()) {
            contributionKey(it.projectItemId, it.amount, it.currency.name, it.date, it.note)
        }
        backup.contributions.forEach { dto ->
            val newProjectItemId = idMap[dto.projectItemLocalId] ?: return@forEach
            val currency = runCatching { Currency.valueOf(dto.currency) }.getOrNull() ?: return@forEach
            val key = contributionKey(newProjectItemId, dto.amount, currency.name, dto.date, dto.note)
            if (!existingContributionKeys.add(key)) return@forEach
            projectRepository.addContribution(
                Contribution(
                    projectItemId = newProjectItemId,
                    amount = dto.amount,
                    currency = currency,
                    date = dto.date,
                    note = dto.note,
                ),
            )
        }

        val existingDependencyPairs = projectRepository.getAllDependencies().mapTo(mutableSetOf()) {
            it.taskId to it.dependsOnTaskId
        }
        backup.dependencies.forEach { dto ->
            val newTaskId = idMap[dto.taskLocalId] ?: return@forEach
            val newDependsOnId = idMap[dto.dependsOnLocalId] ?: return@forEach
            val pair = newTaskId to newDependsOnId
            if (!existingDependencyPairs.add(pair)) return@forEach
            projectRepository.addDependency(newTaskId, newDependsOnId)
        }

        val existingReminderKeys = reminderRepository.getAll().mapTo(mutableSetOf()) {
            reminderKey(it.projectItemId, it.triggerAt, it.message, it.repeatType.name)
        }
        backup.reminders.forEach { dto ->
            val newProjectItemId = idMap[dto.projectItemLocalId] ?: return@forEach
            val repeatType = runCatching { RepeatType.valueOf(dto.repeatType) }.getOrDefault(RepeatType.NONE)
            val key = reminderKey(newProjectItemId, dto.triggerAt, dto.message, repeatType.name)
            if (!existingReminderKeys.add(key)) return@forEach
            val reminderId = reminderRepository.create(
                Reminder(
                    projectItemId = newProjectItemId,
                    triggerAt = dto.triggerAt,
                    message = dto.message,
                    isActive = dto.isActive,
                    repeatType = repeatType,
                ),
            )
            if (dto.isActive) {
                reminderRepository.getById(reminderId)?.let { ReminderScheduler.schedule(appContext, it) }
            }
        }

        ImportResult(projectCount = addedProjects, objectiveCount = addedObjectives)
    }
}
