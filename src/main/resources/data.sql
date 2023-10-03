insert into application_user (version, id, username,name,hashed_password) values (1, '1','user','John Normal','$2a$10$xdbKoM48VySZqVSU/cSlVeJn0Z04XCZ7KZBjUBC00eKo5uLswyOpe')
insert into user_roles (user_id, roles) values ('1', 'USER')
insert into application_user (version, id, username,name,hashed_password) values (1, '2','admin','Emma Powerful','$2a$10$jpLNVNeA7Ar/ZQ2DKbKCm.MuT2ESe.Qop96jipKMq7RaUgCoQedV.')
insert into user_roles (user_id, roles) values ('2', 'USER')
insert into user_roles (user_id, roles) values ('2', 'ADMIN')

create view [dbo].[job_status]
as
WITH
    CTE_Sysession (AgentStartDate)
    AS 
    (
        SELECT MAX(AGENT_START_DATE) AS AgentStartDate FROM MSDB.DBO.SYSSESSIONS
    )   
SELECT sjob.name AS Name
        ,CASE 
            WHEN SJOB.enabled = 1 THEN 'Enabled'
            WHEN sjob.enabled = 0 THEN 'Disabled'
            END AS Job_Enabled
        ,sjob.description AS Job_Description
        ,CASE 
            WHEN ACT.start_execution_date IS NOT NULL AND ACT.stop_execution_date IS NULL  THEN 'Running'
            WHEN ACT.start_execution_date IS NOT NULL AND ACT.stop_execution_date IS NOT NULL AND HIST.run_status = 1 THEN 'Stopped'
            WHEN HIST.run_status = 0 THEN 'Failed'
            WHEN HIST.run_status = 3 THEN 'Canceled'
        END AS Job_Activity
		, DATEDIFF(MINUTE,act.start_execution_date, case when act.stop_execution_date is null then GETDATE() else act.stop_execution_date end ) Duration_Min
        ,act.start_execution_date AS Job_Start_Date
        ,act.last_executed_step_id AS Job_Last_Executed_Step
        ,act.last_executed_step_date AS Job_Executed_Step_Date
        ,act.stop_execution_date AS Job_Stop_Date
        ,act.next_scheduled_run_date AS Job_Next_Run_Date
		  ,CASE WHEN hist.run_status=0 THEN 'Failed'
                     WHEN hist.run_status=1 THEN 'Succeeded'
                     WHEN hist.run_status=2 THEN 'Retry'
                     WHEN hist.run_status=3 THEN 'Cancelled'
               ELSE 'n/a' 
          END [result]
            FROM MSDB.DBO.syssessions AS SYS1
        INNER JOIN CTE_Sysession AS SYS2 ON SYS2.AgentStartDate = SYS1.agent_start_date
        JOIN  msdb.dbo.sysjobactivity act ON act.session_id = SYS1.session_id
        JOIN msdb.dbo.sysjobs sjob ON sjob.job_id = act.job_id
        LEFT JOIN  msdb.dbo.sysjobhistory hist ON hist.job_id = act.job_id AND hist.instance_id = act.job_history_id
GO



INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[Agent_Jobs])

VALUES (1,'CO_ONE',NULL,'Das CO_ONE Besipielprojekt',NULL,NULL)





INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[Agent_Jobs])

VALUES (2,'Teilnehmer',1,'Teilnehmer',NULL,NULL)



INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[Agent_Jobs])

VALUES (3,'PFG_Cube',NULL,'Beschreibung zum PFG-Cube','PFG_Cube','Test;Load_DB1')



INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[Agent_Jobs])

VALUES (4,'CLTV',NULL,'Beschreibung der CLTV Schnittstellen',NULL,NULL)



INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[Agent_Jobs])

VALUES (5,'CLTV',4,'Beschreibung der CLTV Cubes',NULL,NULL)





INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[Agent_Jobs])

VALUES (6,'CLTV_RAW',4,'Beschreibung der CLTV RAW Cubes',NULL,NULL)



INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[Agent_Jobs])

VALUES (7,'CLTV_CHURN',4,'Beschreibung der CLTV CHURN Cubes',NULL,NULL)



INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[Agent_Jobs])

VALUES (8,'CLTV_EOP',4,'Beschreibung der CLTV EOP Cubes',NULL,NULL)
