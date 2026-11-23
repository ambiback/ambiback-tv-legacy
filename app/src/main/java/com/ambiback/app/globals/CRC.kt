package com.ambiback.app.globals

class CRC {
    companion object {
        var CRC_START_8: UByte = 0x00u

        /*
         * static uint8_t sht75_crc_table[];
         *
         * The SHT75 humidity sensor is capable of calculating an 8 bit CRC checksum to
         * ensure data integrity. The lookup table crc_table[] is used to recalculate
         * the CRC.
         */
        var sht75_crc_table: UByteArray = ubyteArrayOf(
            0u,   49u,  98u,  83u,  196u, 245u, 166u, 151u, 185u, 136u, 219u, 234u, 125u, 76u,  31u,  46u,
            67u,  114u, 33u,  16u,  135u, 182u, 229u, 212u, 250u, 203u, 152u, 169u, 62u,  15u,  92u,  109u,
            134u, 183u, 228u, 213u, 66u,  115u, 32u,  17u,  63u,  14u,  93u,  108u, 251u, 202u, 153u, 168u,
            197u, 244u, 167u, 150u, 1u,   48u,  99u,  82u,  124u, 77u,  30u,  47u,  184u, 137u, 218u, 235u,
            61u,  12u,  95u,  110u, 249u, 200u, 155u, 170u, 132u, 181u, 230u, 215u, 64u,  113u, 34u,  19u,
            126u, 79u,  28u,  45u,  186u, 139u, 216u, 233u, 199u, 246u, 165u, 148u, 3u,   50u,  97u,  80u,
            187u, 138u, 217u, 232u, 127u, 78u,  29u,  44u,  2u,   51u,  96u,  81u,  198u, 247u, 164u, 149u,
            248u, 201u, 154u, 171u, 60u,  13u,  94u,  111u, 65u,  112u, 35u,  18u,  133u, 180u, 231u, 214u,
            122u, 75u,  24u,  41u,  190u, 143u, 220u, 237u, 195u, 242u, 161u, 144u, 7u,   54u,  101u, 84u,
            57u,  8u,   91u,  106u, 253u, 204u, 159u, 174u, 128u, 177u, 226u, 211u, 68u,  117u, 38u,  23u,
            252u, 205u, 158u, 175u, 56u,  9u,   90u,  107u, 69u,  116u, 39u,  22u,  129u, 176u, 227u, 210u,
            191u, 142u, 221u, 236u, 123u, 74u,  25u,  40u,  6u,   55u,  100u, 85u,  194u, 243u, 160u, 145u,
            71u,  118u, 37u,  20u,  131u, 178u, 225u, 208u, 254u, 207u, 156u, 173u, 58u,  11u,  88u,  105u,
            4u,   53u,  102u, 87u,  192u, 241u, 162u, 147u, 189u, 140u, 223u, 238u, 121u, 72u,  27u,  42u,
            193u, 240u, 163u, 146u, 5u,   52u,  103u, 86u,  120u, 73u,  26u,  43u,  188u, 141u, 222u, 239u,
            130u, 179u, 224u, 209u, 70u,  119u, 36u,  21u,  59u,  10u,  89u,  104u, 255u, 206u, 157u, 172u
        )

        /*
         * uint8_t crc_8( const unsigned char *input_str, size_t num_bytes );
         *
         * The function crc_8() calculates the 8 bit wide CRC of an input string of a
         * given length.
         */
        fun crc_8( input_bytes: UByteArray, num_bytes: Int, skip_bytes: Int): UByte {
            var a: Int
            var crc: UByte = CRC_START_8

            if ( num_bytes > 0 ) {
                for (a in skip_bytes..(num_bytes-1)) {
                    crc = CRC.sht75_crc_table[(input_bytes[a] xor crc).toInt()];
                }
            }

            return crc;
        }  /* crc_8 */

        /*
         * uint8_t update_crc_8( unsigned char crc, unsigned char val );
         *
         * Given a databyte and the previous value of the CRC value, the function
         * update_crc_8() calculates and returns the new actual CRC value of the data
         * comming in.
         */
        fun update_crc_8( crc: UByte, inchr: UByte): UByte {
            return CRC.sht75_crc_table[(crc xor inchr).toInt()]
        }  /* update_crc_8 */
        // ---
    }

}