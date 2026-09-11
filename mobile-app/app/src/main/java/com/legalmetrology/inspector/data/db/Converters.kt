package com.legalmetrology.inspector.data.db

import androidx.room.TypeConverter
import com.legalmetrology.inspector.domain.model.CommodityCategory
import com.legalmetrology.inspector.domain.model.InspectionStatus
import com.legalmetrology.inspector.domain.model.PackageType

class Converters {
    @TypeConverter fun fromPackageType(v: PackageType): String = v.name
    @TypeConverter fun toPackageType(v: String): PackageType = PackageType.valueOf(v)

    @TypeConverter fun fromCommodityCategory(v: CommodityCategory): String = v.name
    @TypeConverter fun toCommodityCategory(v: String): CommodityCategory = CommodityCategory.valueOf(v)

    @TypeConverter fun fromInspectionStatus(v: InspectionStatus): String = v.name
    @TypeConverter fun toInspectionStatus(v: String): InspectionStatus = InspectionStatus.valueOf(v)
}
