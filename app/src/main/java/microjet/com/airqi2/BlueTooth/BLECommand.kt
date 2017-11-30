package microjet.com.airqi2.BlueTooth

/**
 * Created by B00055 on 2017/11/29.
 */
object Command_List{
    val ReadCmd: Byte = 0xE0.toByte()
    val WriteCmd: Byte = 0xE1.toByte()
    val StopCmd: Byte = 0xEA.toByte()
    val NonStopCmd: Byte = 0xE5.toByte()
    val DeviceID: Byte = 0x50.toByte()
    val SelfTest: Byte =0x51.toByte()
    val PumpOn: Byte=0x52.toByte()
    val Temperature: Byte = 0xA1.toByte()
    val Humidity: Byte = 0xA2.toByte()
    val TVOC: Byte = 0xA3.toByte()
    val CO2: Byte = 0xA4.toByte()
    val Temperature_RAW: Byte =0xA5.toByte()
    val Humidity_RAW: Byte= 0xA6.toByte()
    val TVOC_RAW: Byte =0xA7.toByte()
    val TVOC_Baseline: Byte =0xA8.toByte()
    val NormalLens: Byte =0x02.toByte()
    val SetIDLens: Byte=0x03.toByte()
    val WriteOneByteLens: Byte=0x03.toByte()
    val WriteTwoBytesLens: Byte=0x04.toByte()
    val WriteThreeBytesLens: Byte=0x05.toByte()
    val WriteElevenBytesLens: Byte=0x13.toByte()
    val WriteSixBytesLens: Byte=0x8.toByte()
    val PumpOnLens: Byte=0x04.toByte()
    val SetTVOCLens: Byte=0x07.toByte()
    val GetADCData: Byte=0x53.toByte()
    val GetADCDAtaLens: Byte=0x03.toByte()
    val ResetPumpFreq: Byte=0xA9.toByte()
    val GetPumpFreq: Byte=0xAA.toByte()
    val ResetFlash: Byte=0xAB.toByte()
    val PreHeater: Byte=0xAC.toByte()
    val GetBatteryLife: Byte=0xAD.toByte()
    val FanPower: Byte=0xAE.toByte()
    val SensorOn_OFF: Byte=0xAF.toByte()
    val GetAllFromDevice: Byte=0xB0.toByte()
    val GetInfo: Byte=0xB1.toByte()
    val SetOrGetSampleRate: Byte=0xB2.toByte()
    val CallDeviceStartingGetSample: Byte=0xB3.toByte()
    val GetHistorySampleItems: Byte=0xB4.toByte()
    val GetHistorySample: Byte=0xB5.toByte()
}