Repair Service CRM

Оглавление
1. Frontend	1
2. Telegram Bot Functionality	3
3. Backend REST API	6



1. Frontend 
1.1 Login
Authenticate administrators and managers.
Login using email and password.
JWT authentication.
Redirect to Dashboard after successful authentication.
1.2 Dashboard
Display overall business statistics.
•	Total Requests
•	New Requests
•	Requests In Progress
•	Completed Requests
•	Total Customers
•	Total Technicians
•	Recent Requests
•	Today's Activity
1.3 Requests
Manage repair requests.
•	View all requests
•	Search requests
•	Filter by status
•	Filter by category
•	View request details
•	Assign technician
•	Update request status
•	Delete request
1.4 Request Details
Display complete information about a repair request.
•	Customer information
•	Phone number
•	Repair category
•	Problem description
•	Address
•	Uploaded photo
•	Current status
•	Assigned technician
•	Creation date
•	Update request status
•	Assign technician

1.5 Customers
Manage customer information.
•	Customer list
•	Search customers
•	View customer profile
•	View repair history

1.6 Customer Profile
Display customer details.
•	Full name
•	Phone number
•	Telegram Chat ID
•	Preferred language
•	Total requests
•	Completed requests
•	Repair history
1.7 Technicians
Manage technicians.
•	View technicians
•	Create technician
•	Edit technician
•	Delete technician
•	View assigned requests
1.8 Categories
Manage repair categories.
•	View categories
•	Create category
•	Edit category
•	Delete category
1.9 Review
Display customer feedback.
•	View all reviews
•	Customer rating
•	Customer comment
•	Customer name
•	Related repair request
1.10 Users
Manage administrators and managers.
•	Create user
•	Edit user
•	Delete user
•	Change user role

1.11 Settings
•	Telegram Bot Username

2. Telegram Bot Functionality
2.1 First Launch
The customer starts the bot by sending:
/start
The bot asks the customer to choose a language:
•	🇷🇺 Russian
•	🇺🇿 Uzbek
The selected language is saved in the customer profile.
2.2 Customer Registration
If the customer is not registered:
1.	Enter full name.
2.	Share phone number using the Telegram Contact button.
3.	A new customer profile is created.
2.3 Main Menu
📨 Create Request
📋 My Requests
👤 My Profile
2.4 Create Request Flow
1.	Select repair category.
2.	Enter problem description.
3.	Upload a photo (optional).
4.	Share location or enter address manually.
5.	Review entered information.
6.	Confirm submission.
7.	The bot creates a repair request.
8.	The customer receives a confirmation message with the request number.
Example:
Request #125 created successfully.

2.5 My Requests
Customers can:
•	View all requests.
•	Open request details.
•	Check request status.
•	View repair history.
2.6 My Profile
Customers can:
•	View full name.
•	View phone number.
•	Change full name.
•	Update phone number.
•	Change preferred language.
Statistics displayed:
•	Total Requests
•	Completed Requests
•	Requests In Progress
•	New Requests
2.7 Automatic Notifications
Customers automatically receive Telegram notifications when:
•	Request created
•	Technician assigned
•	Repair started
•	Waiting for spare parts
•	Repair completed
•	Request cancelled
2.8 Customer Review
After a repair is marked as Completed:
1.	The bot asks the customer to rate the service.
2.	The customer selects a rating (1–5 stars).
3.	The customer may optionally leave a comment.
4.	The review is saved and becomes available in the Admin Dashboard.

3. Backend REST API
3.1 Authentication
POST /api/auth/login
Description
Authenticate administrator or manager.
Request
{
  "email": "admin@example.com",
  "password": "password123"
}
Response
{
  "accessToken": "...",
  "refreshToken": "...",
  "expiresIn": 900,
  "user": {
    "id": 1,
    "fullName": "John Smith",
    "role": "ADMIN"
  }
}

POST /api/auth/refresh
Description
Generate a new Access Token using the Refresh Token.
Request
{
  "refreshToken": "..."
}
Response
{
  "accessToken": "...",
  "expiresIn": 900
}

GET /api/auth/me
Description
Return information about the currently authenticated user.
Response
{
  "id": 1,
  "fullName": "John Smith",
  "email": "admin@example.com",
  "role": "ADMIN"
}

3.2 Dashboard
GET /api/dashboard
Description
Return dashboard statistics.
Response
{
  "todayRequests": 15,
  "newRequests": 5,
  "inProgress": 7,
  "completed": 3,
  "totalCustomers": 250,
  "totalTechnicians": 12
}

3.3 Requests
GET /api/requests
Description
Return paginated repair requests.
Response
{
  "content": [],
  "page": 1,
  "size": 20,
  "totalElements": 56
}

GET /api/requests/{id}
Description
Return detailed information about a repair request.
Response
{
  "id": 125,
  "customer": {},
  "category": "Air Conditioner",
  "description": "Not cooling",
  "address": "Tashkent",
  "photoUrl": "...",
  "status": "IN_PROGRESS",
  "technician": {},
  "createdAt": "...",
  "updatedAt": "..."
}

POST /api/requests
Description
Create a new repair request.
Request
{
  "customerId": 10,
  "categoryId": 2,
  "description": "Air conditioner is not cooling.",
  "address": "Tashkent",
  "photoUrl": "...",
  "telegramChatId": 123456789
}
Response
{
  "id": 125,
  "status": "NEW",
  "message": "Request created successfully."
}

PATCH /api/requests/{id}/status
Description
Update repair request status.
Request
{
  "status": "IN_PROGRESS"
}
Response
{
  "message": "Status updated successfully."
}

PATCH /api/requests/{id}/assign
Description
Assign a technician to a repair request.
Request
{
  "technicianId": 4
}
Response
{
  "message": "Technician assigned successfully."
}

3.4 Customers
GET /api/customers
Description
Return all customers.
Response
[
  {
    "id": 1,
    "name": "John Doe",
    "phone": "+998901112233",
    "totalRequests": 8
  }
]

GET /api/customers/{id}
Description
Return customer profile.
Response
{
  "id": 1,
  "name": "John Doe",
  "phone": "+998901112233",
  "telegramChatId": 123456789,
  "language": "EN",
  "totalRequests": 8
}

GET /api/customers/{id}/requests
Description
Return customer's repair history.
Response
[
  {
    "id": 125,
    "status": "COMPLETED",
    "category": "Phone"
  }
]

3.5 Technicians
GET /api/technicians
Description
Return all technicians.
Response
[
  {
    "id": 1,
    "fullName": "Alex",
    "phone": "+998...",
    "active": true,
    "currentRequests": 3
  }
]

POST /api/technicians
Description
Create a new technician.
Request
{
  "fullName": "Alex",
  "phone": "+998..."
}
Response
{
  "id": 1,
  "message": "Technician created successfully."
}
add put  /api/technicians and delete


3.6 Categories
GET /api/categories
Description
Return all repair categories.
Response
[
  {
    "id": 1,
    "name": "Phone"
  }
]
3.7 Reviews
POST /api/requests/{id}/review
Description
Submit a customer review.
Request
{
  "rating": 5,
  "comment": "Excellent service."
}
Response
{
  "message": "Review submitted successfully."
}

GET /api/reviews
Description
Return all customer reviews.
Response
[
  {
    "customer": "John",
    "rating": 5,
    "comment": "Excellent service."
  }
]

3.8 File Upload
POST /api/files/upload
Description
Upload a repair photo.
Request
Multipart file upload.
Response
{
  "url": "https://storage/photo.jpg"
}

3.9 Telegram
POST /api/telegram/webhook
Description
Receive updates from the Telegram Bot API.
Response
HTTP 200 OK

