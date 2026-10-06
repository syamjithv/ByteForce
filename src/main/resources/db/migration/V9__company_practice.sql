-- ByteForce Schema Migration: V9__company_practice.sql
-- Company Practice Module: Company entities and content relationship mappings

-- 1. Companies Master Table
CREATE TABLE IF NOT EXISTS companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(150) NOT NULL UNIQUE,
    logo_path VARCHAR(255) NULL,
    website_url VARCHAR(255) NULL,
    short_description VARCHAR(500) NULL,
    description TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_companies_slug ON companies(slug);
CREATE INDEX idx_companies_active ON companies(active);

-- 2. Company ↔ Topics Relationship Table
CREATE TABLE IF NOT EXISTS company_topics (
    company_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    PRIMARY KEY (company_id, topic_id),
    CONSTRAINT fk_ct_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
);

-- 3. Company ↔ Questions Relationship Table (Coding / Technical Questions)
CREATE TABLE IF NOT EXISTS company_questions (
    company_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    PRIMARY KEY (company_id, question_id),
    CONSTRAINT fk_cq_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_cq_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- 4. Company ↔ Aptitude Questions Relationship Table
CREATE TABLE IF NOT EXISTS company_aptitude_questions (
    company_id BIGINT NOT NULL,
    aptitude_question_id BIGINT NOT NULL,
    PRIMARY KEY (company_id, aptitude_question_id),
    CONSTRAINT fk_caq_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_caq_aptitude FOREIGN KEY (aptitude_question_id) REFERENCES aptitude_questions(id) ON DELETE CASCADE
);

-- 5. Company ↔ Assessments Relationship Table
CREATE TABLE IF NOT EXISTS company_assessments (
    company_id BIGINT NOT NULL,
    assessment_id BIGINT NOT NULL,
    PRIMARY KEY (company_id, assessment_id),
    CONSTRAINT fk_ca_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_ca_assessment FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE
);

-- 6. Company ↔ Interview Preparation Categories Table
CREATE TABLE IF NOT EXISTS company_interview_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    category_type VARCHAR(50) NOT NULL DEFAULT 'TECHNICAL',
    description TEXT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cic_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- 7. Seed Initial 12 Companies (Idempotent)
INSERT INTO companies (name, slug, logo_path, website_url, short_description, description, active, last_reviewed_at)
SELECT 'TCS', 'tcs', '/images/companies/tcs.svg', 'https://www.tcs.com',
       'Global IT services, consulting, and business solutions leader.',
       'Tata Consultancy Services is a global leader in IT services, digital and business solutions.',
       TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE slug = 'tcs');

INSERT INTO companies (name, slug, logo_path, website_url, short_description, description, active, last_reviewed_at)
SELECT 'Infosys', 'infosys', '/images/companies/infosys.svg', 'https://www.infosys.com',
       'Global leader in next-generation digital services and consulting.',
       'Infosys Limited is an Indian multinational information technology company that provides business consulting, information technology and outsourcing services.',
       TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE slug = 'infosys');

INSERT INTO companies (name, slug, logo_path, website_url, short_description, description, active, last_reviewed_at)
SELECT 'Wipro', 'wipro', '/images/companies/wipro.svg', 'https://www.wipro.com',
       'Leading technology services and consulting company focused on innovative solutions.',
       'Wipro Limited is a multinational corporation providing information technology, consulting and business process services headquartered in Bangalore.',
       TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE slug = 'wipro');

INSERT INTO companies (name, slug, logo_path, website_url, short_description, description, active, last_reviewed_at)
SELECT 'HCLTech', 'hcltech', '/images/companies/hcltech.svg', 'https://www.hcltech.com',
       'Global technology company helping enterprises reimagine business in the digital era.',
       'HCLTech is a global technology enterprise providing software development, cloud computing, and digital engineering services.',
       TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE slug = 'hcltech');

INSERT INTO companies (name, slug, logo_path, website_url, short_description, description, active, last_reviewed_at)
SELECT 'Accenture', 'accenture', '/images/companies/accenture.svg', 'https://www.accenture.com',
       'Global professional services company with leading capabilities in digital, cloud, and security.',
       'Accenture is a leading global professional services company providing strategy, consulting, interactive, technology, and operations services.',
       TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE slug = 'accenture');

INSERT INTO companies (name, slug, logo_path, website_url, short_description, description, active, last_reviewed_at)
SELECT 'Cognizant', 'cognizant', '/images/companies/cognizant.svg', 'https://www.cognizant.com',
       'Engineers modern businesses to improve everyday life through digital services.',
       'Cognizant is an American multinational information technology services and consulting company helping clients modernize technology.',
       TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE slug = 'cognizant');

INSERT INTO companies (name, slug, logo_path, website_url, short_description, description, active, last_reviewed_at)
SELECT 'Capgemini', 'capgemini', '/images/companies/capgemini.svg', 'https://www.capgemini.com',
       'Global leader in partnering with companies to transform and manage their business through technology.',
       'Capgemini is a French multinational information technology services and consulting company headquartered in Paris.',
       TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE slug = 'capgemini');

INSERT INTO companies (name, slug, logo_path, website_url, short_description, description, active, last_reviewed_at)
SELECT 'Deloitte', 'deloitte', '/images/companies/deloitte.svg', 'https://www.deloitte.com',
       'Leading global provider of audit, consulting, financial advisory, and risk advisory.',
       'Deloitte provides audit, consulting, financial advisory, risk advisory, tax, and related services to select clients.',
       TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE slug = 'deloitte');

INSERT INTO companies (name, slug, logo_path, website_url, short_description, description, active, last_reviewed_at)
SELECT 'IBM', 'ibm', '/images/companies/ibm.svg', 'https://www.ibm.com',
       'Global technology and cloud company specializing in hybrid cloud and enterprise AI.',
       'International Business Machines Corporation is an American multinational technology corporation headquartered in Armonk, New York.',
       TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE slug = 'ibm');

INSERT INTO companies (name, slug, logo_path, website_url, short_description, description, active, last_reviewed_at)
SELECT 'Amazon', 'amazon', '/images/companies/amazon.svg', 'https://www.amazon.jobs',
       'Global technology leader focused on e-commerce, cloud computing, and digital streaming.',
       'Amazon focuses on e-commerce, cloud computing (AWS), online advertising, digital streaming, and artificial intelligence.',
       TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE slug = 'amazon');

INSERT INTO companies (name, slug, logo_path, website_url, short_description, description, active, last_reviewed_at)
SELECT 'Microsoft', 'microsoft', '/images/companies/microsoft.svg', 'https://careers.microsoft.com',
       'Global technology leader enabling digital transformation for the era of cloud and AI.',
       'Microsoft produces computer software, consumer electronics, personal computers, and related services.',
       TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE slug = 'microsoft');

INSERT INTO companies (name, slug, logo_path, website_url, short_description, description, active, last_reviewed_at)
SELECT 'Google', 'google', '/images/companies/google.svg', 'https://careers.google.com',
       'Global leader in search, cloud systems, artificial intelligence, and computing hardware.',
       'Google focuses on search engine technology, online advertising, cloud computing, computer software, quantum computing, and artificial intelligence.',
       TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE slug = 'google');
