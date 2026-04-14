package com.arcadiapps.localIA.data.db

import androidx.room.TypeConverter
import com.arcadiapps.localIA.data.model.DeviceTier
import com.arcadiapps.localIA.data.model.MessageRole
import com.arcadiapps.localIA.data.model.MessageType
import com.arcadiapps.localIA.data.model.ModelEngine
import com.arcadiapps.localIA.data.model.ModelStatus
import com.arcadiapps.localIA.data.model.ModelType

class Converters {
    @TypeConverter fun toModelType(v: String) = ModelType.valueOf(v)
    @TypeConverter fun fromModelType(v: ModelType) = v.name
    @TypeConverter fun toModelEngine(v: String) = ModelEngine.valueOf(v)
    @TypeConverter fun fromModelEngine(v: ModelEngine) = v.name
    @TypeConverter fun toModelStatus(v: String) = ModelStatus.valueOf(v)
    @TypeConverter fun fromModelStatus(v: ModelStatus) = v.name
    @TypeConverter fun toDeviceTier(v: String) = DeviceTier.valueOf(v)
    @TypeConverter fun fromDeviceTier(v: DeviceTier) = v.name
    @TypeConverter fun toMessageRole(v: String) = MessageRole.valueOf(v)
    @TypeConverter fun fromMessageRole(v: MessageRole) = v.name
    @TypeConverter fun toMessageType(v: String) = MessageType.valueOf(v)
    @TypeConverter fun fromMessageType(v: MessageType) = v.name
}
