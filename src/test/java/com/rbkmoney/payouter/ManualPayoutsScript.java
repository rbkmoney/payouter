package com.rbkmoney.payouter;

import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.payout_processing.GeneratePayoutParams;
import com.rbkmoney.damsel.payout_processing.PayoutManagementSrv;
import com.rbkmoney.damsel.payout_processing.ShopParams;
import com.rbkmoney.damsel.payout_processing.TimeRange;
import com.rbkmoney.woody.api.flow.error.WRuntimeException;
import com.rbkmoney.woody.thrift.impl.http.THSpawnClientBuilder;
import org.apache.thrift.TException;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;

public class ManualPayoutsScript {

    //Запустить, если нужно сформировать выплаты вручную
    @Test
    @Ignore
    public void run() throws FileNotFoundException, URISyntaxException {
        PayoutManagementSrv.Iface payoutManagementSrv = new THSpawnClientBuilder()
                //todo получить доступ и указать ipv6
                .withAddress(new URI("http://payouter:8022/payout/management"))
                .withNetworkTimeout(3000000)
                .build(PayoutManagementSrv.Iface.class);

        //todo указать путь до файла
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream("/Users/n.pospolita/repos/payouter/src/test/resources/payouts_sheduler2.csv")));
        bufferedReader
                .lines()
                .skip(1)
                .map(line -> new AbstractMap.SimpleEntry<>(line.split(",")[0], line.split(",")[1]))
                .forEach(entry -> {
                    System.out.println("partyId=" + entry.getKey() + ", shopId=" + entry.getValue());
                    GeneratePayoutParams generatePayoutParams = new GeneratePayoutParams();

                    //todo указать правильный диапазон формирования выплат
                    generatePayoutParams.setTimeRange(new TimeRange("2019-07-31T17:00:00.000Z", "2019-08-01T17:00:00.000Z"));
                    generatePayoutParams.setShopParams(new ShopParams(entry.getKey(), entry.getValue()));

                    try {
                        boolean withError = true;
                        while(withError) {
                            try {
                                payoutManagementSrv.generatePayouts(generatePayoutParams);
                                System.out.println("Success: partyId=" + entry.getKey() + ", shopId=" + entry.getValue());
                                withError = false;
                            } catch (WRuntimeException ex) {

                            }
                        }
                    } catch (InvalidRequest ex) {
                        System.out.println("Invalid request: partyId=" + entry.getKey() + ", shopId=" + entry.getValue());
                        ex.printStackTrace();
                    } catch (TException ex) {
                        System.out.println("partyId=" + entry.getKey() + ", shopId=" + entry.getValue() + ", error=" + ex);
                        throw new RuntimeException(ex);
                    }
                });
    }
}
