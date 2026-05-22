--
-- PostgreSQL database dump
--

-- Dumped from database version 17.5
-- Dumped by pg_dump version 17.5

-- Started on 2025-09-11 19:57:17

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 222 (class 1259 OID 49866)
-- Name: chat_members; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.chat_members (
    id integer NOT NULL,
    chat_id integer,
    user_id integer,
    joined_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    role character varying(20) DEFAULT 'member'::character varying,
    is_banned boolean DEFAULT false,
    banned_until timestamp with time zone,
    CONSTRAINT chat_members_role_check CHECK (((role)::text = ANY ((ARRAY['owner'::character varying, 'admin'::character varying, 'member'::character varying])::text[])))
);


ALTER TABLE public.chat_members OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 49865)
-- Name: chat_members_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.chat_members_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.chat_members_id_seq OWNER TO postgres;

--
-- TOC entry 4888 (class 0 OID 0)
-- Dependencies: 221
-- Name: chat_members_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.chat_members_id_seq OWNED BY public.chat_members.id;


--
-- TOC entry 220 (class 1259 OID 49844)
-- Name: chats; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.chats (
    id integer NOT NULL,
    name character varying(100),
    description text,
    is_group_chat boolean DEFAULT false,
    is_channel boolean DEFAULT false,
    is_private boolean DEFAULT false,
    created_by integer,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    avatar_url character varying(255),
    last_message_id integer
);


ALTER TABLE public.chats OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 49843)
-- Name: chats_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.chats_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.chats_id_seq OWNER TO postgres;

--
-- TOC entry 4889 (class 0 OID 0)
-- Dependencies: 219
-- Name: chats_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.chats_id_seq OWNED BY public.chats.id;


--
-- TOC entry 226 (class 1259 OID 49926)
-- Name: message_reads; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.message_reads (
    id integer NOT NULL,
    message_id integer,
    user_id integer,
    read_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.message_reads OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 49925)
-- Name: message_reads_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.message_reads_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.message_reads_id_seq OWNER TO postgres;

--
-- TOC entry 4890 (class 0 OID 0)
-- Dependencies: 225
-- Name: message_reads_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.message_reads_id_seq OWNED BY public.message_reads.id;


--
-- TOC entry 224 (class 1259 OID 49892)
-- Name: messages; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.messages (
    id integer NOT NULL,
    chat_id integer,
    sender_id integer,
    content text NOT NULL,
    message_type character varying(20) DEFAULT 'text'::character varying,
    media_url character varying(255),
    file_size integer,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    is_edited boolean DEFAULT false,
    is_deleted boolean DEFAULT false,
    deleted_at timestamp with time zone,
    reply_to_message_id integer,
    encryption_key character varying(255),
    CONSTRAINT messages_message_type_check CHECK (((message_type)::text = ANY ((ARRAY['text'::character varying, 'image'::character varying, 'video'::character varying, 'file'::character varying, 'sticker'::character varying])::text[])))
);


ALTER TABLE public.messages OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 49891)
-- Name: messages_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.messages_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.messages_id_seq OWNER TO postgres;

--
-- TOC entry 4891 (class 0 OID 0)
-- Dependencies: 223
-- Name: messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.messages_id_seq OWNED BY public.messages.id;


--
-- TOC entry 218 (class 1259 OID 49823)
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id integer NOT NULL,
    username character varying(50) NOT NULL,
    email character varying(100) NOT NULL,
    password_hash character varying(255) NOT NULL,
    name character varying(50),
    avatar_url text,
    status character varying(20) DEFAULT 'offline'::character varying,
    last_seen timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    is_verified boolean DEFAULT false,
    CONSTRAINT users_status_check CHECK (((status)::text = ANY ((ARRAY['online'::character varying, 'offline'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- TOC entry 217 (class 1259 OID 49822)
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_id_seq OWNER TO postgres;

--
-- TOC entry 4892 (class 0 OID 0)
-- Dependencies: 217
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- TOC entry 4672 (class 2604 OID 49869)
-- Name: chat_members id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_members ALTER COLUMN id SET DEFAULT nextval('public.chat_members_id_seq'::regclass);


--
-- TOC entry 4666 (class 2604 OID 49847)
-- Name: chats id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chats ALTER COLUMN id SET DEFAULT nextval('public.chats_id_seq'::regclass);


--
-- TOC entry 4682 (class 2604 OID 49929)
-- Name: message_reads id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_reads ALTER COLUMN id SET DEFAULT nextval('public.message_reads_id_seq'::regclass);


--
-- TOC entry 4676 (class 2604 OID 49895)
-- Name: messages id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages ALTER COLUMN id SET DEFAULT nextval('public.messages_id_seq'::regclass);


--
-- TOC entry 4661 (class 2604 OID 49826)
-- Name: users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- TOC entry 4878 (class 0 OID 49866)
-- Dependencies: 222
-- Data for Name: chat_members; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.chat_members (id, chat_id, user_id, joined_at, role, is_banned, banned_until) FROM stdin;
1	1	1	2025-09-01 11:28:59.319312+03	member	f	\N
2	1	2	2025-09-01 11:28:59.319312+03	member	f	\N
3	2	2	2025-09-01 11:28:59.319312+03	member	f	\N
4	2	3	2025-09-01 11:28:59.319312+03	member	f	\N
5	3	1	2025-09-01 11:28:59.319312+03	owner	f	\N
8	3	4	2025-09-01 11:28:59.319312+03	member	f	\N
10	4	4	2025-09-01 11:28:59.319312+03	owner	f	\N
11	4	1	2025-09-01 11:28:59.319312+03	member	f	\N
12	4	2	2025-09-01 11:28:59.319312+03	member	f	\N
13	4	3	2025-09-01 11:28:59.319312+03	member	f	\N
14	4	5	2025-09-01 11:28:59.319312+03	member	f	\N
15	5	3	2025-09-01 11:28:59.319312+03	owner	f	\N
16	5	1	2025-09-01 11:28:59.319312+03	admin	f	\N
17	5	2	2025-09-01 11:28:59.319312+03	member	f	\N
18	5	5	2025-09-01 11:28:59.319312+03	member	f	\N
\.


--
-- TOC entry 4876 (class 0 OID 49844)
-- Dependencies: 220
-- Data for Name: chats; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.chats (id, name, description, is_group_chat, is_channel, is_private, created_by, created_at, updated_at, avatar_url, last_message_id) FROM stdin;
1	\N	\N	f	f	t	1	2025-09-01 11:28:59.275678+03	2025-09-01 11:28:59.275678+03	\N	3
2	\N	\N	f	f	t	2	2025-09-01 11:28:59.275678+03	2025-09-01 11:28:59.275678+03	\N	5
3	Рабочая группа	Обсуждение рабочих вопросов	t	f	f	1	2025-09-01 11:28:59.275678+03	2025-09-01 11:28:59.275678+03	https://example.com/chats/work.jpg	9
4	Новости компании	Официальные объявления	f	t	f	4	2025-09-01 11:28:59.275678+03	2025-09-01 11:28:59.275678+03	https://example.com/chats/news.jpg	10
5	Флудилка	Неформальное общение	t	f	f	3	2025-09-01 11:28:59.275678+03	2025-09-01 11:28:59.275678+03	https://example.com/chats/chat.jpg	15
\.


--
-- TOC entry 4882 (class 0 OID 49926)
-- Dependencies: 226
-- Data for Name: message_reads; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.message_reads (id, message_id, user_id, read_at) FROM stdin;
1	1	2	2025-09-01 10:59:59.405464+03
2	2	1	2025-09-01 11:04:59.405464+03
3	3	2	2025-09-01 11:09:59.405464+03
4	4	3	2025-09-01 08:33:59.405464+03
5	5	2	2025-09-01 08:43:59.405464+03
6	6	2	2025-09-01 09:33:59.405464+03
7	6	3	2025-09-01 09:58:59.405464+03
8	6	4	2025-09-01 09:48:59.405464+03
9	6	5	2025-09-01 09:28:59.405464+03
10	7	1	2025-09-01 09:48:59.405464+03
11	7	2	2025-09-01 09:53:59.405464+03
15	10	1	2025-09-01 06:33:59.405464+03
16	10	2	2025-09-01 06:58:59.405464+03
17	10	3	2025-09-01 07:28:59.405464+03
18	10	4	2025-09-01 06:28:59.405464+03
19	10	5	2025-09-01 08:28:59.405464+03
20	11	1	2025-08-31 12:28:59.405464+03
21	11	2	2025-08-31 13:28:59.405464+03
22	11	3	2025-08-31 15:28:59.405464+03
23	11	4	2025-08-31 11:28:59.405464+03
24	11	5	2025-08-31 17:28:59.405464+03
25	12	1	2025-09-01 08:33:59.405464+03
26	12	2	2025-09-01 08:38:59.405464+03
27	12	5	2025-09-01 08:43:59.405464+03
28	13	1	2025-09-01 08:38:59.405464+03
29	13	2	2025-09-01 08:48:59.405464+03
30	13	3	2025-09-01 08:58:59.405464+03
31	14	2	2025-09-01 09:03:59.405464+03
32	14	3	2025-09-01 09:08:59.405464+03
33	14	5	2025-09-01 09:13:59.405464+03
34	15	1	2025-09-01 09:18:59.405464+03
35	15	3	2025-09-01 09:23:59.405464+03
36	15	5	2025-09-01 09:33:59.405464+03
\.


--
-- TOC entry 4880 (class 0 OID 49892)
-- Dependencies: 224
-- Data for Name: messages; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.messages (id, chat_id, sender_id, content, message_type, media_url, file_size, created_at, updated_at, is_edited, is_deleted, deleted_at, reply_to_message_id, encryption_key) FROM stdin;
1	1	1	Привет, Петр! Как дела?	text	\N	\N	2025-09-01 10:58:59.355421+03	2025-09-01 11:28:59.355421+03	f	f	\N	\N	\N
2	1	2	Привет! Все отлично, спасибо!	text	\N	\N	2025-09-01 11:03:59.355421+03	2025-09-01 11:28:59.355421+03	f	f	\N	\N	\N
3	1	1	Отлично! Что насчет встречи завтра?	text	\N	\N	2025-09-01 11:08:59.355421+03	2025-09-01 11:28:59.355421+03	f	f	\N	\N	\N
4	2	2	Мария, привет!	text	\N	\N	2025-09-01 08:28:59.355421+03	2025-09-01 11:28:59.355421+03	f	f	\N	\N	\N
5	2	3	Привет, Петр! Чем могу помочь?	text	\N	\N	2025-09-01 08:38:59.355421+03	2025-09-01 11:28:59.355421+03	f	f	\N	\N	\N
6	3	1	Коллеги, доброе утро!	text	\N	\N	2025-09-01 09:28:59.355421+03	2025-09-01 11:28:59.355421+03	f	f	\N	\N	\N
7	3	4	Всех с началом недели!	text	\N	\N	2025-09-01 09:43:59.355421+03	2025-09-01 11:28:59.355421+03	f	f	\N	\N	\N
10	4	4	Важное объявление: завтра технические работы с 23:00 до 02:00	text	\N	\N	2025-09-01 06:28:59.355421+03	2025-09-01 11:28:59.355421+03	f	f	\N	\N	\N
11	4	4	Поздравляем с днем рождения Елену из отдела маркетинга!	text	\N	\N	2025-08-31 11:28:59.355421+03	2025-09-01 11:28:59.355421+03	f	f	\N	\N	\N
12	5	3	Кто смотрел новый сериал?	text	\N	\N	2025-09-01 08:28:59.355421+03	2025-09-01 11:28:59.355421+03	f	f	\N	\N	\N
13	5	5	Я! Очень крутой!	text	\N	\N	2025-09-01 08:33:59.355421+03	2025-09-01 11:28:59.355421+03	f	f	\N	\N	\N
14	5	1	Какие планы на выходные?	text	\N	\N	2025-09-01 08:58:59.355421+03	2025-09-01 11:28:59.355421+03	f	f	\N	\N	\N
15	5	2	Предлагаю собраться всем!	text	\N	\N	2025-09-01 09:28:59.355421+03	2025-09-01 11:28:59.355421+03	f	f	\N	\N	\N
30	5	5	sdfdsfds	text	\N	\N	2025-09-04 14:51:50.59784+03	2025-09-04 14:51:50.59784+03	f	f	\N	\N	\N
31	4	5	dfgfdgdfgdf	text	\N	\N	2025-09-04 14:51:53.42484+03	2025-09-04 14:51:53.42484+03	f	f	\N	\N	\N
32	4	5	sfsdfsd	text	\N	\N	2025-09-04 22:53:22.633276+03	2025-09-04 22:53:22.633276+03	f	f	\N	\N	\N
33	4	5	fdgfdg	text	\N	\N	2025-09-04 23:00:53.500211+03	2025-09-04 23:00:53.500211+03	f	f	\N	\N	\N
34	5	5	asdas	text	\N	\N	2025-09-04 23:01:01.408321+03	2025-09-04 23:01:01.408321+03	f	f	\N	\N	\N
35	4	5	dsfsdfs	text	\N	\N	2025-09-07 12:09:30.542517+03	2025-09-07 12:09:30.542517+03	f	f	\N	\N	\N
36	5	5	sdfsdfs	text	\N	\N	2025-09-07 12:09:40.535518+03	2025-09-07 12:09:40.535518+03	f	f	\N	\N	\N
\.


--
-- TOC entry 4874 (class 0 OID 49823)
-- Dependencies: 218
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, username, email, password_hash, name, avatar_url, status, last_seen, created_at, is_verified) FROM stdin;
1	aesty	denis@gmail.com	$2a$12$5qF0lO3eoBANivdVVBAEd.SfhjEDA01KQiq5xvBCMLZy11lNMVHZi	Raul Raulev	\N	\N	2025-08-30 19:37:28.194467+03	2025-08-30 19:37:28.194467+03	f
2	kadirov	denis123@gmail.com	$2a$12$27az6Aag6zarSsgo.OYZfucMDsrP3yXEWPso0e55cKp.JZOlWTxSu	Denis Kadirov	\N	\N	2025-09-01 11:27:47.038678+03	2025-09-01 11:27:47.038678+03	f
3	deniska	kadirovdenis082@gmail.com	$2a$12$5fZdCV49Tl4eW73MqOfmyOH73MjWiwBGELihWyqOK2dDUj0d15GyG	Raul Raulevdd	\N	\N	2025-09-01 11:28:09.141747+03	2025-09-01 11:28:09.141747+03	f
7	smirnov	smirnov@example.com	$2a$12$5qF0lO3eoBANivdVVBAEd.SfhjEDA01KQiq5xvBCMLZy11lNMVHZi	Алексей Смирнов	https://example.com/avatars/smirnov.jpg	online	2025-09-01 11:28:58.897182+03	2025-09-01 11:28:58.897182+03	t
4	ivanov	ivanov@example.com	$2a$12$5qF0lO3eoBANivdVVBAEd.SfhjEDA01KQiq5xvBCMLZy11lNMVHZi	Иван Иванов	https://example.com/avatars/ivanov.jpg	online	2025-09-01 11:28:58.897182+03	2025-09-01 11:28:58.897182+03	t
6	sidorova	sidorova@example.com	$2a$12$5qF0lO3eoBANivdVVBAEd.SfhjEDA01KQiq5xvBCMLZy11lNMVHZi	Мария Сидорова	https://example.com/avatars/sidorova.jpg	offline	2025-09-01 09:28:58.897182+03	2025-09-01 11:28:58.897182+03	f
8	kuznetsova	kuznetsova@example.com	$2a$12$5qF0lO3eoBANivdVVBAEd.SfhjEDA01KQiq5xvBCMLZy11lNMVHZi	Елена Кузнецова	https://example.com/avatars/kuznetsova.jpg	offline	2025-08-31 11:28:58.897182+03	2025-09-01 11:28:58.897182+03	f
5	petrovv	petrov@example.com	$2a$12$7piXrUdQQ6MRixOK8AAg0u4WJUDXK2NS34veh7Y/R70v05goq5PNu	Петр Петровв	https://api.dicebear.com/7.x/avataaars/svg?seed=Kai	online	2025-09-01 11:28:58.897182+03	2025-09-01 11:28:58.897182+03	t
\.


--
-- TOC entry 4893 (class 0 OID 0)
-- Dependencies: 221
-- Name: chat_members_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.chat_members_id_seq', 18, true);


--
-- TOC entry 4894 (class 0 OID 0)
-- Dependencies: 219
-- Name: chats_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.chats_id_seq', 5, true);


--
-- TOC entry 4895 (class 0 OID 0)
-- Dependencies: 225
-- Name: message_reads_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.message_reads_id_seq', 36, true);


--
-- TOC entry 4896 (class 0 OID 0)
-- Dependencies: 223
-- Name: messages_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.messages_id_seq', 36, true);


--
-- TOC entry 4897 (class 0 OID 0)
-- Dependencies: 217
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 8, true);


--
-- TOC entry 4702 (class 2606 OID 49877)
-- Name: chat_members chat_members_chat_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_members
    ADD CONSTRAINT chat_members_chat_id_user_id_key UNIQUE (chat_id, user_id);


--
-- TOC entry 4704 (class 2606 OID 49875)
-- Name: chat_members chat_members_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_members
    ADD CONSTRAINT chat_members_pkey PRIMARY KEY (id);


--
-- TOC entry 4697 (class 2606 OID 49856)
-- Name: chats chats_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chats
    ADD CONSTRAINT chats_pkey PRIMARY KEY (id);


--
-- TOC entry 4717 (class 2606 OID 49934)
-- Name: message_reads message_reads_message_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_reads
    ADD CONSTRAINT message_reads_message_id_user_id_key UNIQUE (message_id, user_id);


--
-- TOC entry 4719 (class 2606 OID 49932)
-- Name: message_reads message_reads_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_reads
    ADD CONSTRAINT message_reads_pkey PRIMARY KEY (id);


--
-- TOC entry 4713 (class 2606 OID 49905)
-- Name: messages messages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_pkey PRIMARY KEY (id);


--
-- TOC entry 4691 (class 2606 OID 49839)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 4693 (class 2606 OID 49835)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 4695 (class 2606 OID 49837)
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- TOC entry 4705 (class 1259 OID 49888)
-- Name: idx_chat_members_chat_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chat_members_chat_id ON public.chat_members USING btree (chat_id);


--
-- TOC entry 4706 (class 1259 OID 49890)
-- Name: idx_chat_members_role; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chat_members_role ON public.chat_members USING btree (role);


--
-- TOC entry 4707 (class 1259 OID 49889)
-- Name: idx_chat_members_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chat_members_user_id ON public.chat_members USING btree (user_id);


--
-- TOC entry 4698 (class 1259 OID 49862)
-- Name: idx_chats_created_by; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chats_created_by ON public.chats USING btree (created_by);


--
-- TOC entry 4699 (class 1259 OID 49863)
-- Name: idx_chats_is_group; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chats_is_group ON public.chats USING btree (is_group_chat);


--
-- TOC entry 4700 (class 1259 OID 49864)
-- Name: idx_chats_updated_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chats_updated_at ON public.chats USING btree (updated_at);


--
-- TOC entry 4714 (class 1259 OID 49946)
-- Name: idx_message_reads_message_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_message_reads_message_id ON public.message_reads USING btree (message_id);


--
-- TOC entry 4715 (class 1259 OID 49945)
-- Name: idx_message_reads_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_message_reads_user_id ON public.message_reads USING btree (user_id);


--
-- TOC entry 4708 (class 1259 OID 49921)
-- Name: idx_messages_chat_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_messages_chat_id ON public.messages USING btree (chat_id);


--
-- TOC entry 4709 (class 1259 OID 49923)
-- Name: idx_messages_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_messages_created_at ON public.messages USING btree (created_at);


--
-- TOC entry 4710 (class 1259 OID 49924)
-- Name: idx_messages_reply_to; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_messages_reply_to ON public.messages USING btree (reply_to_message_id);


--
-- TOC entry 4711 (class 1259 OID 49922)
-- Name: idx_messages_sender_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_messages_sender_id ON public.messages USING btree (sender_id);


--
-- TOC entry 4687 (class 1259 OID 49841)
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- TOC entry 4688 (class 1259 OID 49842)
-- Name: idx_users_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_status ON public.users USING btree (status);


--
-- TOC entry 4689 (class 1259 OID 49840)
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_username ON public.users USING btree (username);


--
-- TOC entry 4721 (class 2606 OID 49878)
-- Name: chat_members chat_members_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_members
    ADD CONSTRAINT chat_members_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.chats(id) ON DELETE CASCADE;


--
-- TOC entry 4722 (class 2606 OID 49883)
-- Name: chat_members chat_members_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_members
    ADD CONSTRAINT chat_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 4720 (class 2606 OID 49857)
-- Name: chats chats_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chats
    ADD CONSTRAINT chats_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 4726 (class 2606 OID 49935)
-- Name: message_reads message_reads_message_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_reads
    ADD CONSTRAINT message_reads_message_id_fkey FOREIGN KEY (message_id) REFERENCES public.messages(id) ON DELETE CASCADE;


--
-- TOC entry 4727 (class 2606 OID 49940)
-- Name: message_reads message_reads_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_reads
    ADD CONSTRAINT message_reads_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 4723 (class 2606 OID 49906)
-- Name: messages messages_chat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_chat_id_fkey FOREIGN KEY (chat_id) REFERENCES public.chats(id) ON DELETE CASCADE;


--
-- TOC entry 4724 (class 2606 OID 49916)
-- Name: messages messages_reply_to_message_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_reply_to_message_id_fkey FOREIGN KEY (reply_to_message_id) REFERENCES public.messages(id) ON DELETE SET NULL;


--
-- TOC entry 4725 (class 2606 OID 49911)
-- Name: messages messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.messages
    ADD CONSTRAINT messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id) ON DELETE SET NULL;


-- Completed on 2025-09-11 19:57:18

--
-- PostgreSQL database dump complete
--

