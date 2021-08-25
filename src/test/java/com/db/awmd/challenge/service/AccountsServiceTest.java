package com.db.awmd.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransferMoneyDTO;
import com.db.awmd.challenge.exception.AccountDoesntExistException;
import com.db.awmd.challenge.exception.AccountNegativeBalanceException;
import com.db.awmd.challenge.exception.AmountTransferGreaterThanZeroException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.SameAccountException;
import com.db.awmd.challenge.service.AccountsService;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

    @Autowired
    private AccountsService accountsService;

    static final Logger logger = LoggerFactory.getLogger(AccountsServiceTest.class);

    @Test
    public void addAccount() throws Exception {
        Account account = new Account("Id-123");
        account.setBalance(new BigDecimal(1000));
        this.accountsService.createAccount(account);

        assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
    }

    @Test
    public void addAccount_failsOnDuplicateId() throws Exception {
        String uniqueId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueId);
        this.accountsService.createAccount(account);

        try {
            this.accountsService.createAccount(account);
            fail("Should have failed when adding duplicate account");
        } catch (DuplicateAccountIdException ex) {
            assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
        }

    }

    @Test
    public void transferMoneyBetweenAccounts()
            throws AmountTransferGreaterThanZeroException, AccountNegativeBalanceException, AccountDoesntExistException, SameAccountException {
        Account accountFromTransfer = new Account("Id-321", new BigDecimal("100.00"));
        Account accountToTransfer = new Account("Id-322", new BigDecimal("110.00"));
        accountsService.createAccount(accountFromTransfer);
        accountsService.createAccount(accountToTransfer);
        TransferMoneyDTO transferMoneyDTO = new TransferMoneyDTO(accountFromTransfer.getAccountId(), accountToTransfer.getAccountId(), new BigDecimal("60"));

        accountsService.transferMoneyBetweenAccounts(transferMoneyDTO);

        assertEquals(0, accountsService.getAccount(accountFromTransfer.getAccountId()).getBalance().compareTo(BigDecimal.valueOf(40)));
        assertEquals(0, accountsService.getAccount(accountToTransfer.getAccountId()).getBalance().compareTo(BigDecimal.valueOf(170)));
    }

    @Test(expected = AccountNegativeBalanceException.class)
    public void transferMoneyBetweenAccounts_NoFounds() throws Exception {
        Account accountFromTransfer = new Account("Id-323", new BigDecimal("50.00"));
        Account accountToTransfer = new Account("Id-324", new BigDecimal("110.00"));
        accountsService.createAccount(accountFromTransfer);
        accountsService.createAccount(accountToTransfer);

        TransferMoneyDTO transferMoneyDTO = new TransferMoneyDTO(accountFromTransfer.getAccountId(), accountToTransfer.getAccountId(), new BigDecimal("60"));

        accountsService.transferMoneyBetweenAccounts(transferMoneyDTO);
    }

    @Test(expected = AmountTransferGreaterThanZeroException.class)
    public void transferMoneyBetweenAccounts_NegativeTransfer() throws Exception {
        Account accountFromTransfer = new Account("Id-325", new BigDecimal("50.00"));
        Account accountToTransfer = new Account("Id-326", new BigDecimal("110.00"));
        accountsService.createAccount(accountFromTransfer);
        accountsService.createAccount(accountToTransfer);

        TransferMoneyDTO transferMoneyDTO = new TransferMoneyDTO(accountFromTransfer.getAccountId(), accountToTransfer.getAccountId(), new BigDecimal("-60"));

        accountsService.transferMoneyBetweenAccounts(transferMoneyDTO);
    }

    @Test(expected = AccountDoesntExistException.class)
    public void transferMoneyBetweenAccounts_AccountDoesNotExist() throws Exception {
        Account accountFromTransfer = new Account("Id-327", new BigDecimal("100.00"));
        accountsService.createAccount(accountFromTransfer);

        TransferMoneyDTO transferMoneyDTO = new TransferMoneyDTO(accountFromTransfer.getAccountId(), "Id-328", new BigDecimal("60"));

        accountsService.transferMoneyBetweenAccounts(transferMoneyDTO);
    }

    @Test(expected = SameAccountException.class)
    public void transferMoneyBetweenAccounts_SameDestinationAndOriginAccount() throws Exception {
        Account accountFromTransfer = new Account("Id-328", new BigDecimal("100.00"));
        accountsService.createAccount(accountFromTransfer);

        TransferMoneyDTO transferMoneyDTO = new TransferMoneyDTO(accountFromTransfer.getAccountId(), accountFromTransfer.getAccountId(), new BigDecimal("60"));

        accountsService.transferMoneyBetweenAccounts(transferMoneyDTO);
    }

    @Test
    public void transferMoneyBetweenAccounts_ConcurrentThreads() throws InterruptedException {
        Account accountFromTransfer = new Account("330", new BigDecimal("100.00"));
        Account accountToTransfer = new Account("331", new BigDecimal("110.00"));
        accountsService.createAccount(accountFromTransfer);
        accountsService.createAccount(accountToTransfer);

        TransferMoneyDTO transferMoneyDTO = new TransferMoneyDTO(accountFromTransfer.getAccountId(), accountToTransfer.getAccountId(), new BigDecimal("20"));

        callConcurrentThreads(transferMoneyDTO, 20);

        assertEquals(0, accountFromTransfer.getBalance().compareTo(BigDecimal.valueOf(0)));
        assertEquals(0, accountToTransfer.getBalance().compareTo(BigDecimal.valueOf(210)));
    }

    private void callConcurrentThreads(TransferMoneyDTO transferMoneyDTO, int numberOfThreads) throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            service.execute(() -> {
                try {
                    accountsService.transferMoneyBetweenAccounts(transferMoneyDTO);
                } catch (AccountDoesntExistException | AmountTransferGreaterThanZeroException
                        | AccountNegativeBalanceException | SameAccountException e) {
                    logger.info(e.getMessage());
                }
                latch.countDown();
            });
        }
        latch.await();
    }
}
