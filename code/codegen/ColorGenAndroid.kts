#!/usr/bin/env kscript

//DEPS org.redundent:kotlin-xml-builder:1.5.1
//INCLUDE ../PPPShared/src/commonMain/kotlin/ui/PPPColor.kt

import org.redundent.kotlin.xml.*
import java.io.File

val colors = PPPColor.values()

val colorResXML = xml("resources") {
    for (color in colors) {
        color?.let {
            "color" {
                -it.hexColor
            attribute("name", color.name.decapitalize())
            }
        }
    }
}

val printOptions = PrintOptions(pretty = true, singleLineTextElements = true)
val xmlString = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + colorResXML.toString(printOptions = printOptions)

val currentDirectory = File("").getAbsoluteFile()
val colorXMLPath = File(currentDirectory, "../android/src/main/res/values/colors.xml")
colorXMLPath.writeText(xmlString)

println("Generated Android colors!")
