package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransferMoneyDTO;
import com.db.awmd.challenge.exception.AccountDoesntExistException;
import com.db.awmd.challenge.exception.AccountNegativeBalanceException;
import com.db.awmd.challenge.exception.AmountTransferGreaterThanZeroException;
import com.db.awmd.challenge.exception.SameAccountException;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.web.AccountsController;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccountsService {

    @Getter
    private final AccountsRepository accountsRepository;

    @Getter
    private final NotificationService notificationService;

    @Autowired
    public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
        this.accountsRepository = accountsRepository;
        this.notificationService = notificationService;
    }

    public void createAccount(Account account) {
        this.accountsRepository.createAccount(account);
    }

    public Account getAccount(String accountId) {
        return this.accountsRepository.getAccount(accountId);
    }

    public ResponseEntity<Object> transferMoneyBetweenAccounts(TransferMoneyDTO transferMoneyDTO)
            throws AccountDoesntExistException, AmountTransferGreaterThanZeroException, SameAccountException, AccountNegativeBalanceException {

		Account accountFromTransfer = getAccount(transferMoneyDTO.getAccountFromId());
		Account accountToTransfer = getAccount(transferMoneyDTO.getAccountToId());

		exceptionValidator(accountFromTransfer, accountToTransfer, transferMoneyDTO);

		Object lock1 = accountToTransfer.getReentrantLock();
		Object lock2 = accountFromTransfer.getReentrantLock();

		synchronized (lock1) {
			synchronized (lock2) {
				log.info("New Thread");
				log.info("Initial Balance From: {}", accountFromTransfer.getBalance());
				log.info("Initial Balance To: {}", accountToTransfer.getBalance());

				if (fromAccountBalanceIsValid(accountFromTransfer, transferMoneyDTO)) {
					throw new AccountNegativeBalanceException("Account doesn't have the balance to transfer");
				}

                transferMoneyAndNotifyUsers(accountFromTransfer,accountToTransfer,transferMoneyDTO);

				log.info("Final Balance From: {}", accountFromTransfer.getBalance());
				log.info("Final Balance To: {}", accountToTransfer.getBalance());
			}
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

    private void exceptionValidator(Account accountFromTransfer, Account accountToTransfer, TransferMoneyDTO transferMoneyDTO)
            throws AccountDoesntExistException, AmountTransferGreaterThanZeroException, SameAccountException {
        if (Objects.isNull(accountFromTransfer) || Objects.isNull(accountToTransfer)) {
            throw new AccountDoesntExistException("One of the accounts does not exist");
        }

        if (accountFromTransfer.getAccountId().equals(accountToTransfer.getAccountId())) {
            throw new SameAccountException("Origin account and destination account should not be the same");
        }

        if (transferMoneyDTO.getAmountToTransfer().signum() <= 0) {
            throw new AmountTransferGreaterThanZeroException("Transfer amount should be greater than 0");
        }
    }

    private boolean fromAccountBalanceIsValid(Account accountFromTransfer, TransferMoneyDTO transferMoneyDTO) {
        return accountFromTransfer.getBalance().subtract(transferMoneyDTO.getAmountToTransfer()).signum() < 0;
    }

    private void transferMoneyAndNotifyUsers(Account accountFromTransfer, Account accountToTransfer, TransferMoneyDTO transferMoneyDTO) {
        accountFromTransfer.setBalance(accountFromTransfer.getBalance().subtract(transferMoneyDTO.getAmountToTransfer()));
        accountToTransfer.setBalance(accountToTransfer.getBalance().add(transferMoneyDTO.getAmountToTransfer()));

        List<Account> accountsToSave = new ArrayList<>();
        accountsToSave.add(accountFromTransfer);
        accountsToSave.add(accountToTransfer);
        accountsRepository.updateAccounts(accountsToSave);

        notificationService.notifyAboutTransfer(accountFromTransfer, transferMoneyDTO.getAmountToTransfer().toString());
        notificationService.notifyAboutTransfer(accountToTransfer, transferMoneyDTO.getAmountToTransfer().toString());
    }

}