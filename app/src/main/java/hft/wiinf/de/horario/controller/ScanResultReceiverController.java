package hft.wiinf.de.horario.controller;

public interface ScanResultReceiverController {

    void scanResultData(String codeFormat, String codeContent);

    void scanResultData(NoScanResultExceptionController noScanData);
}
