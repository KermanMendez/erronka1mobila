package com.example.erronka1.db

import android.util.Xml
import com.example.erronka1.model.User
import com.example.erronka1.model.Workout
import java.io.File
import java.io.FileOutputStream
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class Backup {

    fun <T : Any> writeListToXml(tagName: String, list: List<T>, serializer: org.xmlpull.v1.XmlSerializer, indent: String = "    ") {
        serializer.text("\n$indent")
        serializer.startTag(null, tagName)
        for (item in list) {
            val itemTag = item::class.simpleName?.lowercase() ?: "item"
            serializer.text("\n$indent$indent")
            serializer.startTag(null, itemTag)
            try {
                val kClass = item::class
                for (prop in kClass.memberProperties) {
                    try {
                        val kProp = prop as? KProperty1<T, *>
                        val value = kProp?.get(item)?.toString() ?: ""
                        serializer.attribute(null, prop.name, value)
                    } catch (e: Exception) {
                        android.util.Log.e("Backup", "Error reading property '${prop.name}' from $itemTag: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Backup", "Reflection error for $itemTag: ${e.message}")
            }
            serializer.endTag(null, itemTag)
        }
        serializer.text("\n$indent")
        serializer.endTag(null, tagName)
    }

    fun writeBackupToXml(workouts: List<Workout>, users: List<User>, filesDir: File, fileName: String) {
        val dbDir = File(filesDir, "db")
        if (!dbDir.exists()) dbDir.mkdirs()
        val file = File(dbDir, fileName)
        val fos = FileOutputStream(file)
        val serializer = Xml.newSerializer()
        serializer.setOutput(fos, "UTF-8")
        serializer.startDocument("UTF-8", true)
        serializer.text("\n")
        serializer.startTag(null, "backup")

        writeListToXml("workouts", workouts, serializer, "    ")
        writeListToXml("users", users, serializer, "    ")

        serializer.text("\n")
        serializer.endTag(null, "backup")
        serializer.endDocument()
        fos.close()
    }
}
