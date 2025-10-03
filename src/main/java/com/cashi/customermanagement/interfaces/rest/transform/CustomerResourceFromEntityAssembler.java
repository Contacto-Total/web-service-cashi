package com.cashi.customermanagement.interfaces.rest.transform;

import com.cashi.customermanagement.domain.model.aggregates.Customer;
import com.cashi.customermanagement.interfaces.rest.resources.*;

public class CustomerResourceFromEntityAssembler {

    public static CustomerResource toResourceFromEntity(Customer entity) {
        return new CustomerResource(
                entity.getId(),
                entity.getCustomerId(),
                entity.getFullName(),
                entity.getDocumentNumber() != null ? entity.getDocumentNumber().getType() : null,
                entity.getDocumentNumber() != null ? entity.getDocumentNumber().getNumber() : null,
                entity.getBirthDate(),
                entity.getAge(),
                toContactInfoResource(entity),
                toAccountInfoResource(entity),
                toDebtInfoResource(entity)
        );
    }

    private static ContactInfoResource toContactInfoResource(Customer entity) {
        if (entity.getContactInfo() == null) return null;
        var contact = entity.getContactInfo();
        return new ContactInfoResource(
                contact.getPrimaryPhone(),
                contact.getAlternativePhone(),
                contact.getWorkPhone(),
                contact.getEmail(),
                contact.getAddress()
        );
    }

    private static AccountInfoResource toAccountInfoResource(Customer entity) {
        if (entity.getAccountInfo() == null) return null;
        var account = entity.getAccountInfo();
        return new AccountInfoResource(
                account.getAccountNumber(),
                account.getProductType(),
                account.getDisbursementDate(),
                account.getOriginalAmount(),
                account.getTermMonths(),
                account.getInterestRate()
        );
    }

    private static DebtInfoResource toDebtInfoResource(Customer entity) {
        if (entity.getDebtInfo() == null) return null;
        var debt = entity.getDebtInfo();
        return new DebtInfoResource(
                debt.getCapitalBalance(),
                debt.getOverdueInterest(),
                debt.getAccumulatedLateFees(),
                debt.getCollectionFees(),
                debt.getTotalBalance(),
                debt.getDaysOverdue(),
                debt.getLastPaymentDate(),
                debt.getLastPaymentAmount()
        );
    }
}
