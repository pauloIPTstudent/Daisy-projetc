import argparse

from tables import app, User, db

def seed(email: str, password: str):
    with app.app_context():
        existing = User.query.filter_by(email=email).first()
        if existing:
            print(f"User already exists: {email} (id={existing.id})")
            print(f"token={existing.get_token()}")
            return existing

        user = User()
        user.create_account(email, password)
        db.session.add(user)
        db.session.commit()
        print(f"Created user: {email} (id={user.id})")
        print(f"token={user.get_token()}")
        return user


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Seed DB with example user')
    parser.add_argument('--email', default='user@example.com', help='user email')
    parser.add_argument('--password', default='secret123', help='user password')
    args = parser.parse_args()
    seed(args.email, args.password)


