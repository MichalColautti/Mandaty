CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    service_number TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL
);

CREATE TABLE tickets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    driver_name TEXT NOT NULL,
    pesel TEXT NOT NULL,
    offense TEXT NOT NULL,
    fine_amount REAL NOT NULL,
    penalty_points INTEGER NOT NULL,
    issued_by INTEGER NOT NULL,
    issue_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (issued_by) REFERENCES users (id)
);

CREATE TABLE driver(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pesel TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL
);

--insert into users values (1,'123nypd','pass');
--select * from tickets;
