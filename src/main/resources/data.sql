insert into application_user (version, id, username,name,hashed_password) values (1, '1','user','John Normal','$2a$10$xdbKoM48VySZqVSU/cSlVeJn0Z04XCZ7KZBjUBC00eKo5uLswyOpe')
insert into user_roles (user_id, roles) values ('1', 'USER')
insert into application_user (version, id, username,name,hashed_password) values (1, '2','admin','Emma Powerful','$2a$10$jpLNVNeA7Ar/ZQ2DKbKCm.MuT2ESe.Qop96jipKMq7RaUgCoQedV.')
insert into user_roles (user_id, roles) values ('2', 'USER')
insert into user_roles (user_id, roles) values ('2', 'ADMIN')

INSERT INTO "DEPARTMENT" (ID, NAME, PARENT_ID, DESCRIPTION, PAGE_URL) VALUES
(1, 'CO_ONE', NULL, 'Päivi', 'HW-Mapping'),
(11, 'Teilnehmer', 1, 'Pekka', 'kb'),
(111, 'Anforderung', 11, 'Pekka', 'PFG-Mapping'),
(112, 'QS', 11, 'Gilberto', NULL),
(12, 'Usage', 1, 'Pekka', NULL),
(121, 'Anforderung', 12, 'Thomas', NULL),
(122, 'QS', 12, 'Tomi', NULL),
(2, 'HR', NULL, 'Anne', NULL),
(21, 'Office', 2, 'Anu', NULL),
(22, 'Employee', 2, 'Minna', NULL),
(3, 'Marketing', NULL, 'Niko', NULL),
(31, 'Growth', 3, 'Ömer', NULL),
(32, 'Demand Generation', 3, 'Marcus', NULL),
(33, 'Product Marketing', 3, 'Pekka', NULL),
(34, 'Brand Experience', 3, 'Eero', NULL);



CREATE TABLE [dbo].[Projects](

                                 [ID] [int] NOT NULL,

                                 [Parent_ID] [int] NULL,

                                 [Name] [varchar](255) NOT NULL,

                                 [Description] [varchar](max) NOT NULL,

                                 [Page_URL] [varchar](255) NULL,

                                 [AgentJobs] [varchar](255) NULL

) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]



INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[AgentJobs])

VALUES (1,'CO_ONE',NULL,'Das CO_ONE Besipielprojekt',NULL,NULL)





INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[AgentJobs])

VALUES (2,'Teilnehmer',1,'Teilnehmer',NULL,NULL)



INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[AgentJobs])

VALUES (3,'PFG_Cube',NULL,'Beschreibung zum PFG-Cube','PFG_Cube','Test;Load_DB1')



INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[AgentJobs])

VALUES (4,'CLTV',NULL,'Beschreibung der CLTV Schnittstellen',NULL,NULL)



INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[AgentJobs])

VALUES (5,'CLTV',4,'Beschreibung der CLTV Cubes',NULL,NULL)





INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[AgentJobs])

VALUES (6,'CLTV_RAW',4,'Beschreibung der CLTV RAW Cubes',NULL,NULL)



INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[AgentJobs])

VALUES (7,'CLTV_CHURN',4,'Beschreibung der CLTV CHURN Cubes',NULL,NULL)



INSERT INTO [dbo].[Projects]

([ID]

,[Name]

,[Parent_ID]

,[Description]

,[Page_URL]

,[AgentJobs])

VALUES (8,'CLTV_EOP',4,'Beschreibung der CLTV EOP Cubes',NULL,NULL)
