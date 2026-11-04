/*
 * Hamoni: Cloud workflow platform
 * Copyright (C) 2026  Hamoni Limited
 *
 * Unless you have received this program directly from Hamoni Limited pursuant to
 * the terms of a commercial license agreement with Hamoni Limited, then this
 * program is licensed to you under the terms of version 3 of the GNU Affero
 * General Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ai.hamoni.common.core.repository

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/*
 * Standard entity converters for JPA.
 */

/**
 * Converter to convert OffsetDateTime (set the JPA driver) into an Instant to avoid timezone issues.
 */
@Converter(autoApply = true)
class OffsetDateTimeToInstantConverter : AttributeConverter<Instant?, OffsetDateTime?> {
    override fun convertToDatabaseColumn(attribute: Instant?): OffsetDateTime? =
        if (attribute == null) null else OffsetDateTime.ofInstant(attribute, ZoneOffset.UTC)

    override fun convertToEntityAttribute(dbData: OffsetDateTime?): Instant? =
        dbData?.toInstant()
}
