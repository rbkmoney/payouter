package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.payouter.exception.InvalidStateException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.model.PayoutToolData;
import com.rbkmoney.payouter.service.PartyManagementService;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PartyManagementServiceImpl implements PartyManagementService {

    private final UserInfo userInfo = new UserInfo("admin", UserType.internal_user(new InternalUser()));

    private final PartyManagementSrv.Iface partyManagementSrv;

    @Autowired
    public PartyManagementServiceImpl(PartyManagementSrv.Iface partyManagementSrv) {
        this.partyManagementSrv = partyManagementSrv;
    }

    @Override
    public Shop getShop(String partyId, String shopId) throws NotFoundException {
        try {
            return partyManagementSrv.getShop(userInfo, partyId, shopId);
        } catch (PartyNotFound | ShopNotFound ex) {
            throw new NotFoundException(
                    String.format("Shop not found, partyId='%s', contractId='%s'", partyId, shopId), ex
            );
        } catch (TException ex) {
            throw new RuntimeException(
                    String.format("Failed to get shop, partyId='%s', shopId='%s'", partyId, shopId), ex
            );
        }
    }

    @Override
    public Contract getContract(String partyId, String contractId) throws NotFoundException {
        try {
            return partyManagementSrv.getContract(userInfo, partyId, contractId);
        } catch (PartyNotFound | ContractNotFound ex) {
            throw new NotFoundException(
                    String.format("Contract not found, partyId='%s', contractId='%s'", partyId, contractId),
                    ex);
        } catch (TException ex) {
            throw new RuntimeException(
                    String.format("Failed to get contract, partyId='%s', contractId='%s'", partyId, contractId),
                    ex);
        }
    }

    @Override
    public PayoutToolData getPayoutToolData(String partyId, String shopId) throws InvalidStateException, NotFoundException {
        Shop shop = getShop(partyId, shopId);

        PayoutToolData payoutToolData = new PayoutToolData();

        ShopAccount shopAccount = shop.getAccount();
        payoutToolData.setShopAccountId(shopAccount.getSettlement());
        payoutToolData.setShopPayoutAccountId(shopAccount.getPayout());
        payoutToolData.setCurrencyCode(shopAccount.getCurrency().getSymbolicCode());


        Contract contract = getContract(partyId, shop.getContractId());

        Optional<PayoutTool> payoutToolOptional = contract.getPayoutTools().stream()
                .filter(payoutTool ->
                        payoutTool.getPayoutToolInfo().isSetBankAccount()
                                && payoutTool.getId().equals(shop.getPayoutToolId()))
                .findFirst();

        if (!payoutToolOptional.isPresent()) {
            throw new NotFoundException(
                    String.format("Payout tool with bank account not found, partyId='%s', shopId='%s', payoutToolId='%s'",
                            partyId, shopId, shop.getPayoutToolId()));
        }

        PayoutTool payoutTool = payoutToolOptional.get();
        if (!payoutToolData.getCurrencyCode().equals(payoutTool.getCurrency().getSymbolicCode())) {
            throw new InvalidStateException("Shop account and payout tool currency must be equals");
        }

        BankAccount bankAccount = payoutTool.getPayoutToolInfo().getBankAccount();

        payoutToolData.setBankAccount(bankAccount.getAccount());
        payoutToolData.setBankBik(bankAccount.getBankBik());
        payoutToolData.setBankName(bankAccount.getBankName());
        payoutToolData.setBankPostAccount(bankAccount.getBankPostAccount());

        if (contract.getContractor().getLegalEntity().isSetRussianLegalEntity()) {
            RussianLegalEntity russianLegalEntity = contract.getContractor().getLegalEntity().getRussianLegalEntity();
            payoutToolData.setInn(russianLegalEntity.getInn());
            payoutToolData.setDescription(russianLegalEntity.getRegisteredName());
        }

        if (!contract.isSetLegalAgreement()) {
            throw new NotFoundException(
                    String.format("Legal agreement not found, partyId='%s', shopId='%s', contractId='%s'",
                            partyId, shopId, contract.getId()));
        }

        payoutToolData.setLegalAgreementId(contract.getLegalAgreement().getLegalAgreementId());
        payoutToolData.setLegalAgreementSignedAt(
                TypeUtil.stringToLocalDateTime(contract.getLegalAgreement().getSignedAt())
        );

        return payoutToolData;
    }
}
