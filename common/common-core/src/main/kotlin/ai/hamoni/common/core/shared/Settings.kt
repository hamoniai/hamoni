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

package ai.hamoni.common.core.shared

import java.util.UUID

/*
 * Shared interfaces for the settings structure.
 */

/**
 * A settings value: its type, its stored-text conversion and the value used
 * when no row is stored.
 */
interface TypedSetting {
    /** The value type and its conversion. */
    val type: TypedSettingType<*>

    /** The value used when the settings table holds no row. */
    val default: Any

    /** Decode [value] using the key's type. */
    fun decode(value: String): Any = type.decode(value)

    /** Encode [value] using the key's type. */
    fun encode(value: Any): String = type.encode(value)
}

/**
 * A supported setting value type and its stored-text conversion.
 *
 * The concrete type is retained for the typed [serializer] and [deserializer].
 * The non-generic [encode] and [decode] methods are the bridge used when the
 * type is held through [TypedSetting.type], where the concrete type is erased.
 */
sealed interface TypedSettingType<T : Any> {
    /** Decode to the type's value, returned existentially. */
    fun decode(value: String): Any = deserializer(value)

    /** Convert stored text back to the value. */
    fun deserializer(value: String): T

    /** Encode a value whose concrete type is known only by the key. */
    @Suppress("UNCHECKED_CAST")
    fun encode(value: Any): String = serializer(value as T)

    /** Convert [value] to its stored text. */
    fun serializer(value: T): String

    /** The type for a [Boolean] value. */
    data object BooleanType : TypedSettingType<Boolean> {
        override fun deserializer(value: String): Boolean = value.toBooleanStrict()

        override fun serializer(value: Boolean): String = value.toString()
    }

    /** The type for a [String] value. */
    data object StringType : TypedSettingType<String> {
        override fun deserializer(value: String): String = value

        override fun serializer(value: String): String = value
    }

    /** The type for a [UUID] value. */
    data object UUIDType : TypedSettingType<UUID> {
        override fun deserializer(value: String): UUID = UUID.fromString(value)

        override fun serializer(value: UUID): String = value.toString()
    }
}
