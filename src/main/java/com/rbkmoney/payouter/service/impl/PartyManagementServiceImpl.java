package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.msgpack.Value;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.payouter.exception.InvalidStateException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.model.PayoutToolData;
import com.rbkmoney.payouter.service.DominantService;
import com.rbkmoney.payouter.service.PartyManagementService;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class PartyManagementServiceImpl implements PartyManagementService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final UserInfo userInfo = new UserInfo("admin", UserType.internal_user(new InternalUser()));

    private final PartyManagementSrv.Iface partyManagementClient;

    private final DominantService dominantService;

    @Autowired
    public PartyManagementServiceImpl(PartyManagementSrv.Iface partyManagementClient, DominantService dominantService) {
        this.partyManagementClient = partyManagementClient;
        this.dominantService = dominantService;
    }

    @Override
    public Party getParty(String partyId) throws NotFoundException {
        return getParty(partyId, Instant.now());
    }

    @Override
    public Party getParty(String partyId, Instant timestamp) throws NotFoundException {
        return getParty(partyId, PartyRevisionParam.timestamp(TypeUtil.temporalToString(timestamp)));
    }

    @Override
    public Party getParty(String partyId, long partyRevision) throws NotFoundException {
        return getParty(partyId, PartyRevisionParam.revision(partyRevision));
    }

    @Override
    public Party getParty(String partyId, PartyRevisionParam partyRevisionParam) throws NotFoundException {
        log.debug("Trying to get party, partyId='{}', partyRevisionParam='{}'", partyId, partyRevisionParam);
        try {
            Party party = partyManagementClient.checkout(userInfo, partyId, partyRevisionParam);
            log.info("Party has been found, partyId='{}', partyRevisionParam='{}'", partyId, partyRevisionParam);
            return party;
        } catch (PartyNotFound ex) {
            throw new NotFoundException(
                    String.format("Party not found, partyId='%s', partyRevisionParam='%s'", partyId, partyRevisionParam), ex
            );
        } catch (InvalidPartyRevision ex) {
            throw new NotFoundException(
                    String.format("Invalid party revision, partyId='%s', partyRevisionParam='%s'", partyId, partyRevisionParam), ex
            );
        } catch (TException ex) {
            throw new RuntimeException(
                    String.format("Failed to get party, partyId='%s', partyRevisionParam='%s'", partyId, partyRevisionParam), ex
            );
        }
    }

    @Override
    public Shop getShop(String partyId, String shopId) throws NotFoundException {
        return getShop(partyId, shopId, Instant.now());
    }

    @Override
    public Shop getShop(String partyId, String shopId, long partyRevision) throws NotFoundException {
        return getShop(partyId, shopId, PartyRevisionParam.revision(partyRevision));
    }

    @Override
    public Shop getShop(String partyId, String shopId, Instant timestamp) throws NotFoundException {
        return getShop(partyId, shopId, PartyRevisionParam.timestamp(TypeUtil.temporalToString(timestamp)));
    }

    @Override
    public Shop getShop(String partyId, String shopId, PartyRevisionParam partyRevisionParam) throws NotFoundException {
        log.debug("Trying to get shop, partyId='{}', shopId='{}', partyRevisionParam='{}'", partyId, shopId, partyRevisionParam);
        Party party = getParty(partyId, partyRevisionParam);

        Shop shop = party.getShops().get(shopId);
        if (shop == null) {
            throw new NotFoundException(String.format("Shop not found, partyId='%s', shopId='%s', partyRevisionParam='%s'", partyId, shopId, partyRevisionParam));
        }
        log.info("Shop has been found, partyId='{}', shopId='{}', partyRevisionParam='{}'", partyId, shopId, partyRevisionParam);
        return shop;
    }

    @Override
    public Shop getShopByContractAndPayoutToolIds(String partyId, String contractId, String payoutToolId) throws NotFoundException {
        log.debug("Trying to get shop by contract and payoutTool ids, partyId='{}', contractId='{}', payoutToolId='{}'", partyId, contractId, payoutToolId);
        Party party = getParty(partyId);

        Shop shop = party.getShops().values().stream()
                .filter(
                        shopValue -> shopValue.getContractId().equals(contractId)
                                && shopValue.getPayoutToolId().equals(payoutToolId)
                ).findFirst().orElseThrow(() -> new NotFoundException(
                        String.format("Shop not found, partyId='{}', contractId='{}', payoutToolId='{}'", party, contractId, payoutToolId)
                ));

        log.info("Shop has been found, partyId='{}', contractId='{}', payoutToolId='{}'", partyId, contractId, payoutToolId);
        return shop;
    }

    @Override
    public TermSet computeShopTerms(String partyId, String shopId) throws NotFoundException {
        return computeShopTerms(partyId, shopId, Instant.now());
    }

    @Override
    public TermSet computeShopTerms(String partyId, String shopId, Instant timestamp) throws NotFoundException {
        log.debug("Trying to compute shop terms, partyId='{}', shopId='{}', timestamp='{}'", partyId, shopId, timestamp);
        try {
            TermSet termSet = partyManagementClient.computeShopTerms(userInfo, partyId, shopId, TypeUtil.temporalToString(timestamp));
            log.info("Shop terms has been computed, partyId='{}', shopId='{}', timestamp='{}', terms='{}'", partyId, shopId, timestamp, termSet);
            return termSet;
        } catch (PartyNotFound | PartyNotExistsYet | ShopNotFound ex) {
            throw new NotFoundException(
                    String.format("%s, partyId='%s', shopId='%s', timestamp='%s'", ex.getClass().getSimpleName(), partyId, shopId, timestamp),
                    ex);
        } catch (TException ex) {
            throw new RuntimeException(
                    String.format("Failed to compute shop terms, partyId='%s', shopId='%s', timestamp='%s'", partyId, shopId, timestamp), ex
            );
        }
    }

    @Override
    public Contract getContract(String partyId, String contractId) throws NotFoundException {
        return getContract(partyId, contractId, Instant.now());
    }

    @Override
    public Contract getContract(String partyId, String contractId, long partyRevision) throws NotFoundException {
        return getContract(partyId, contractId, PartyRevisionParam.revision(partyRevision));
    }

    @Override
    public Contract getContract(String partyId, String contractId, Instant timestamp) throws NotFoundException {
        return getContract(partyId, contractId, PartyRevisionParam.timestamp(TypeUtil.temporalToString(timestamp)));
    }

    @Override
    public Contract getContract(String partyId, String contractId, PartyRevisionParam partyRevisionParam) throws NotFoundException {
        log.debug("Trying to get contract, partyId='{}', contractId='{}', partyRevisionParam='{}'", partyId, contractId, partyRevisionParam);
        Party party = getParty(partyId, partyRevisionParam);

        Contract contract = party.getContracts().get(contractId);
        if (contract == null) {
            throw new NotFoundException(String.format("Shop not found, partyId='%s', contractId='%s', partyRevisionParam='%s'", partyId, contractId, partyRevisionParam));
        }
        log.info("Contract has been found, partyId='{}', contractId='{}', partyRevisionParam='{}'", partyId, contractId, partyRevisionParam);
        return contract;
    }

    @Override
    public PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId) throws NotFoundException {
        return getPaymentInstitutionRef(partyId, contractId, Instant.now());
    }

    @Override
    public PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId, long partyRevision) throws NotFoundException {
        return getPaymentInstitutionRef(partyId, contractId, PartyRevisionParam.revision(partyRevision));
    }

    @Override
    public PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId, Instant timestamp) throws NotFoundException {
        return getPaymentInstitutionRef(partyId, contractId, PartyRevisionParam.timestamp(TypeUtil.temporalToString(timestamp)));
    }

    @Override
    public PaymentInstitutionRef getPaymentInstitutionRef(String partyId, String contractId, PartyRevisionParam partyRevisionParam) throws NotFoundException {
        log.debug("Trying to get paymentInstitutionRef, partyId='{}', contractId='{}', partyRevisionParam='{}'", partyId, contractId, partyRevisionParam);
        Contract contract = getContract(partyId, contractId, partyRevisionParam);

        if (!contract.isSetPaymentInstitution()) {
            throw new NotFoundException(String.format("PaymentInstitutionRef not found, partyId='%s', contractId='%s', partyRevisionParam='%s'", partyId, contractId, partyRevisionParam));
        }

        PaymentInstitutionRef paymentInstitutionRef = contract.getPaymentInstitution();
        log.info("PaymentInstitutionRef has been found, partyId='{}', contractId='{}', paymentInstitutionRef='{}', partyRevisionParam='{}'", partyId, contractId, paymentInstitutionRef, partyRevisionParam);
        return paymentInstitutionRef;
    }

    @Override
    public Value getMetaData(String partyId, String namespace) throws NotFoundException {
        try {
            return partyManagementClient.getMetaData(userInfo, partyId, namespace);
        } catch (PartyMetaNamespaceNotFound ex) {
            return null;
        } catch (PartyNotFound ex) {
            throw new NotFoundException(
                    String.format("Party not found, partyId='%s', namespace='%s'", partyId, namespace),
                    ex
            );
        } catch (TException ex) {
            throw new RuntimeException(
                    String.format("Failed to get namespace, partyId='%s', namespace='%s'", partyId, namespace), ex
            );
        }
    }

    @Override
    public PayoutToolData getPayoutToolData(String partyId, String shopId) throws InvalidStateException, NotFoundException {
        return getPayoutToolData(partyId, shopId, Instant.now());
    }

    @Override
    public PayoutToolData getPayoutToolData(String partyId, String shopId, Instant timestamp) throws InvalidStateException, NotFoundException {
        log.debug("Trying to get payout tool data, partyId='{}', shopId='{}', timestamp='{}'", partyId, shopId, timestamp);
        Party party = getParty(partyId, timestamp);

        Shop shop = party.getShops().get(shopId);
        if (shop == null) {
            throw new NotFoundException(String.format("Shop not found, partyId='%s', contractId='%s', timestamp='%s'", partyId, shopId, timestamp));
        }

        PayoutToolData payoutToolData = new PayoutToolData();

        ShopAccount shopAccount = shop.getAccount();
        payoutToolData.setShopAccountId(shopAccount.getSettlement());
        payoutToolData.setShopPayoutAccountId(shopAccount.getPayout());
        payoutToolData.setCurrencyCode(shopAccount.getCurrency().getSymbolicCode());


        Contract contract = party.getContracts().get(shop.getContractId());
        if (contract == null) {
            throw new NotFoundException(String.format("Contract not found, partyId='%s', contractId='%s', timestamp='%s'", partyId, shop.getId(), timestamp));
        }

        Optional<PayoutTool> payoutToolOptional = contract.getPayoutTools().stream()
                .filter(payoutTool ->
                        payoutTool.getPayoutToolInfo().isSetRussianBankAccount()
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

        RussianBankAccount bankAccount = payoutTool.getPayoutToolInfo().getRussianBankAccount();

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
        payoutToolData.setPurpose(
                String.format(
                        "Перевод согласно договора номер %s от %s.  Без НДС",
                        payoutToolData.getLegalAgreementId(),
                        payoutToolData.getLegalAgreementSignedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                )
        );

        log.info("Payout tool data has been found, partyId='{}', shopId='{}', timestamp='{}', payoutToolData='{}'", partyId, shopId, timestamp, payoutToolData);
        return payoutToolData;
    }

    @Override
    public CategoryType getCategoryType(String partyId, String shopId, long domainRevision, Instant timestamp) throws NotFoundException {
        log.debug("Trying to get shop category type, partyId='{}', timestamp='{}'", partyId, timestamp);
        Shop shop = getShop(partyId, shopId, timestamp);

        CategoryType categoryType = dominantService.getCategoryType(shop.getCategory(), domainRevision);
        log.info("Shop category type has been found, categoryType='{}', partyId='{}', timestamp='{}'", categoryType, partyId, timestamp);
        return categoryType;
    }

    @Override
    public boolean isTestCategoryType(String partyId, String shopId, long domainRevision, Instant timestamp) throws NotFoundException {
        return getCategoryType(partyId, shopId, domainRevision, timestamp) == CategoryType.test;
    }

}
