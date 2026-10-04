db = db.getSiblingDB('officyna');

const adminExists = db.users.findOne({ email: 'admin@officyna.com' });

if (!adminExists) {
    db.users.insertOne({
        name: 'Administrador',
        email: 'admin@officyna.com',
        password: '$2a$10$/9mUYeaeYYBseqldjp5Yaem4vYeEbQQGvKZjfaEuQoPm0vzzd5ra.',
        userRole: 'ADMIN',
        active: true,
        createdAt: new Date(),
        updatedAt: new Date(),
        _class: 'br.com.officyna.administrative.user.domain.UserEntity'
    });
    print('Admin user created.');
} else {
    print('Admin user already exists, skipping.');
}