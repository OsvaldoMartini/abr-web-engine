package com.allinweb.ch.dto;

import com.allinweb.ch.util.ABRConstants;
import com.allinweb.ch.util.ABRPropertyEnum;
import com.allinweb.ch.util.ABRPropertyManager;
import java.io.File;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

public class Repository {

    private static final String CONNECTION_TYPE = "jdbc:ucanaccess://";
    private static final String CONNECTION_PARAMETERS = ";memory=false;newDatabaseVersion=V2010";

    private SessionFactory sessionFactory = null;
    private Session session = null;

    public Repository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        openSession();
    }

    private void openSession() {
        if (sessionFactory == null) {
            String dbPath = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.FOLDER_PATH_DB);
            File dbFolder = new File(dbPath);
            dbFolder.mkdirs();
            String dbUrl = CONNECTION_TYPE + dbPath + ABRConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
            sessionFactory = new Configuration()
                    .configure()
                    .setProperty("hibernate.connection.url", dbUrl)
                    .buildSessionFactory();
        }
        if (session == null || !session.isOpen()) {
            session = sessionFactory.openSession();
        }
    }

    public <T> void write(T obj) {
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            session.save(obj);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    private void checkIdDtoToRetrieve(int id) throws Exception {
        if (id == 0) {
            throw new Exception("ID of DTO to retrieve cannot be 0");
        }
    }

    public HomeBankingDTO retrieveHomeBankingDTOById(int id) throws Exception {
        checkIdDtoToRetrieve(id);
        return session.get(HomeBankingDTO.class, id);
    }

    public BotJobDTO retrieveBotJobDTOById(int id) throws Exception {
        checkIdDtoToRetrieve(id);
        return session.get(BotJobDTO.class, id);
    }

    public BlockDTO retrieveBlockDTOById(int id) throws Exception {
        checkIdDtoToRetrieve(id);
        return session.get(BlockDTO.class, id);
    }

    public BlockLoopInstructionDTO retrieveBlockLoopInstructionDTOById(int id) throws Exception {
        checkIdDtoToRetrieve(id);
        return session.get(BlockLoopInstructionDTO.class, id);
    }

    public BaseDTO getParentDto(BaseDTO childDto) throws Exception {
        int id = childDto.getId();
        BaseDTO parentDTO = null;

        if (childDto instanceof BlockLoopInstructionDTO) {
            BlockLoopInstructionDTO blockLoopInstructionDTO = (BlockLoopInstructionDTO) childDto;
            if (blockLoopInstructionDTO.getBlock() == null) {
                blockLoopInstructionDTO = retrieveBlockLoopInstructionDTOById(id);
            }
            parentDTO = retrieveBlockDTOById(blockLoopInstructionDTO.getBlock().getId());
        } else if (childDto instanceof BlockDTO) {
            BlockDTO blockDTO = (BlockDTO) childDto;
            if (blockDTO.getBotJob() == null) {
                blockDTO = retrieveBlockDTOById(id);
            }
            parentDTO = retrieveBotJobDTOById(blockDTO.getBotJob().getId());
        } else if (childDto instanceof BotJobDTO) {
            BotJobDTO botJobDTO = (BotJobDTO) childDto;
            if (botJobDTO.getHomeBanking() == null) {
                botJobDTO = retrieveBotJobDTOById(id);
            }
            parentDTO = retrieveHomeBankingDTOById(botJobDTO.getHomeBanking().getId());
        } else if (childDto instanceof HomeBankingDTO) {
            throw new Exception("HomeBanking has no parent");
        }

        return parentDTO;
    }

    public void closeSession() {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }
}
